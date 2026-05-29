package com.saving.auth.service;

import com.saving.auth.dto.request.CreateUserRequest;
import com.saving.auth.dto.request.LoginRequest;
import com.saving.auth.dto.request.RefreshTokenRequest;
import com.saving.auth.dto.request.VerifyOtpRequest;
import com.saving.auth.dto.response.CreateUserResponse;
import com.saving.auth.dto.response.LoginResponse;
import com.saving.auth.dto.response.TokenValidationResponse;
import com.saving.auth.dto.response.UserInfoResponse;
import com.saving.auth.entity.*;
import com.saving.auth.exception.BusinessException;
import com.saving.auth.exception.ErrorCode;
import com.saving.auth.repository.LoginSessionRepository;
import com.saving.auth.repository.OtpRequestRepository;
import com.saving.auth.repository.RoleRepository;
import com.saving.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository         userRepository;
    private final LoginSessionRepository sessionRepository;
    private final OtpRequestRepository   otpRepository;
    private final RoleRepository         roleRepository;
    private final JwtService             jwtService;
    private final AuthenticationManager  authenticationManager;
    private final PasswordEncoder        passwordEncoder;

    // ── LOGIN ─────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        log.info("[LOGIN] Step 1/5 — authenticating user '{}'", username);

        // 1. Authenticate via Spring Security
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("[LOGIN] FAILED — bad credentials for user '{}'", username);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. Load user with roles
        log.info("[LOGIN] Step 2/5 — loading user record for '{}'", username);
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toList());
        log.info("[LOGIN] Step 2/5 — user loaded: id={} cif={} roles={} status={}",
                user.getUserId(), user.getCif(), roles, user.getStatus());

        // 3. Generate tokens
        log.info("[LOGIN] Step 3/5 — generating JWT tokens");
        String userId = user.getUserId().toString();
        String accessToken  = jwtService.generateAccessToken(userId, username, user.getCif(), roles);
        String refreshToken = jwtService.generateRefreshToken(userId, username);

        // 4. Persist login session
        log.info("[LOGIN] Step 4/5 — saving login session | ip={}", request.getClientIp());
        LoginSession session = LoginSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(refreshToken))
                .ipAddress(request.getClientIp())
                .deviceInfo(request.getDeviceInfo())
                .expiresAt(Instant.now().plusMillis(jwtService.getAccessExpirationMs() * 24))
                .isRevoked(false)
                .build();
        sessionRepository.save(session);

        // 5. Update last login timestamp
        userRepository.updateLastLoginAt(user.getUserId(), Instant.now());

        log.info("[LOGIN] Step 5/5 — SUCCESS for '{}' | roles={}", username, roles);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .userId(userId)
                .username(username)
                .cif(user.getCif())
                .roles(roles)
                .build();
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("[REFRESH] Step 1/4 — validating refresh token signature");

        // 1. Validate token signature and type
        if (!jwtService.isTokenValid(request.getRefreshToken())
                || !jwtService.isRefreshToken(request.getRefreshToken())) {
            log.warn("[REFRESH] FAILED — invalid or expired refresh token");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. Verify session is active in DB
        log.info("[REFRESH] Step 2/4 — looking up session in database");
        String tokenHash = hashToken(request.getRefreshToken());
        LoginSession session = sessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[REFRESH] FAILED — no session found for token hash");
                    return new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Session not found");
                });

        if (!session.isValid()) {
            log.warn("[REFRESH] FAILED — session {} is revoked or expired", session.getSessionId());
            throw new BusinessException(ErrorCode.SESSION_REVOKED, "Session is revoked or expired");
        }

        // 3. Load user
        String username = jwtService.extractUsername(request.getRefreshToken());
        log.info("[REFRESH] Step 3/4 — loading user '{}'", username);
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            log.warn("[REFRESH] FAILED — account '{}' is not active (status={})", username, user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toList());

        // 4. Rotate tokens: revoke old session, issue new tokens, save new session
        log.info("[REFRESH] Step 4/4 — rotating tokens (revoking session {})", session.getSessionId());
        String userId = user.getUserId().toString();
        String newAccessToken  = jwtService.generateAccessToken(userId, username, user.getCif(), roles);
        String newRefreshToken = jwtService.generateRefreshToken(userId, username);

        sessionRepository.revokeById(session.getSessionId(), Instant.now());

        LoginSession newSession = LoginSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(newRefreshToken))
                .ipAddress(session.getIpAddress())
                .deviceInfo(session.getDeviceInfo())
                .expiresAt(Instant.now().plusMillis(jwtService.getAccessExpirationMs() * 24))
                .isRevoked(false)
                .build();
        sessionRepository.save(newSession);

        log.info("[REFRESH] SUCCESS for '{}' | roles={}", username, roles);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .userId(userId)
                .username(username)
                .cif(user.getCif())
                .roles(roles)
                .build();
    }

    // ── VALIDATE TOKEN (internal — called by other services) ──────

    public TokenValidationResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("[VALIDATE] Rejected — token is missing");
            return TokenValidationResponse.invalid("TOKEN_MISSING");
        }

        Map<String, Object> result = jwtService.validateAndExtract(token);

        if (!Boolean.TRUE.equals(result.get("valid"))) {
            String reason = (String) result.get("reason");
            log.warn("[VALIDATE] Rejected — reason: {}", reason);
            return TokenValidationResponse.invalid(reason);
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) result.getOrDefault("roles", List.of());
        String username = (String) result.get("username");

        log.debug("[VALIDATE] OK — user='{}' roles={}", username, roles);

        return TokenValidationResponse.builder()
                .valid(true)
                .userId((String) result.get("userId"))
                .username(username)
                .cif((String) result.get("cif"))
                .roles(roles)
                .expiresAt((Long) result.get("expiresAt"))
                .build();
    }

    // ── VERIFY OTP ────────────────────────────────────────────────

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        log.info("[OTP] Verify request — userId={} purpose={}", request.getUserId(), request.getPurpose());

        User user = userRepository.findById(UUID.fromString(request.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        OtpRequest otp = otpRepository.findLatestValidOtp(
                user.getUserId(), request.getPurpose(), Instant.now())
                .orElseThrow(() -> {
                    log.warn("[OTP] FAILED — no valid OTP found for userId={} purpose={}", request.getUserId(), request.getPurpose());
                    return new BusinessException(ErrorCode.OTP_INVALID, "No valid OTP found for user and purpose");
                });

        boolean valid = passwordEncoder.matches(request.getOtpCode(), otp.getOtpCodeHash());
        if (!valid) {
            log.warn("[OTP] FAILED — code mismatch for userId={}", request.getUserId());
            throw new BusinessException(ErrorCode.OTP_INVALID, "OTP code does not match");
        }

        otpRepository.markAsUsed(otp.getOtpId());
        log.info("[OTP] SUCCESS — userId={} purpose={}", request.getUserId(), request.getPurpose());
        return true;
    }

    // ── LOGOUT ────────────────────────────────────────────────────

    @Transactional
    public void logout(String username) {
        log.info("[LOGOUT] Request for '{}'", username);
        userRepository.findByUsername(username).ifPresent(user -> {
            int revoked = sessionRepository.revokeAllByUserId(user.getUserId(), Instant.now());
            log.info("[LOGOUT] SUCCESS — '{}' | sessions revoked: {}", username, revoked);
        });
    }

    // ── GET CURRENT USER ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String username) {
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .userId(user.getUserId().toString())
                .username(user.getUsername())
                .cif(user.getCif())
                .status(user.getStatus())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    // ── CREATE USER (staff-only) ──────────────────────────────────

    /**
     * Create a CUSTOMER login account linked to an existing CIF.
     * Called by TELLER or ADMIN after they have already created the customer record.
     */
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("[CREATE_USER] Step 1/4 — checking uniqueness: username='{}' cif='{}'",
                request.getUsername(), request.getCif());

        // 1. Username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("[CREATE_USER] FAILED — username '{}' is already taken", request.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_TAKEN,
                    "Username '" + request.getUsername() + "' is already in use");
        }

        // 2. One login account per CIF
        if (userRepository.findByCif(request.getCif()).isPresent()) {
            log.warn("[CREATE_USER] FAILED — CIF '{}' already has a login account", request.getCif());
            throw new BusinessException(ErrorCode.CIF_ALREADY_HAS_USER,
                    "CIF " + request.getCif() + " already has a login account");
        }

        // 3. Resolve CUSTOMER role
        log.info("[CREATE_USER] Step 2/4 — resolving CUSTOMER role");
        Role customerRole = roleRepository.findByRoleCode("CUSTOMER")
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND,
                        "Role CUSTOMER not found in database"));

        // 4. Build and persist User
        log.info("[CREATE_USER] Step 3/4 — persisting user and assigning role");
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .cif(request.getCif())
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();
        user.getUserRoles().add(userRole);
        userRepository.save(user);

        log.info("[CREATE_USER] Step 4/4 — SUCCESS: userId={} username='{}' cif='{}'",
                user.getUserId(), user.getUsername(), user.getCif());

        return CreateUserResponse.builder()
                .userId(user.getUserId().toString())
                .username(user.getUsername())
                .cif(user.getCif())
                .role("CUSTOMER")
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // Fallback: use last 64 chars
            return token.length() > 64 ? token.substring(token.length() - 64) : token;
        }
    }
}
