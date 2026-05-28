package com.saving.auth.service;

import com.saving.auth.dto.request.LoginRequest;
import com.saving.auth.dto.request.RefreshTokenRequest;
import com.saving.auth.dto.request.VerifyOtpRequest;
import com.saving.auth.dto.response.LoginResponse;
import com.saving.auth.dto.response.TokenValidationResponse;
import com.saving.auth.dto.response.UserInfoResponse;
import com.saving.auth.entity.*;
import com.saving.auth.exception.BusinessException;
import com.saving.auth.exception.ErrorCode;
import com.saving.auth.repository.LoginSessionRepository;
import com.saving.auth.repository.OtpRequestRepository;
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
    private final JwtService             jwtService;
    private final AuthenticationManager  authenticationManager;
    private final PasswordEncoder        passwordEncoder;

    // ── LOGIN ─────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        // 1. Authenticate
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Bad credentials for user: {}", request.getUsername());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. Load user with roles
        User user = userRepository.findByUsernameWithRoles(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toList());

        // 3. Generate tokens
        String userId = user.getUserId().toString();
        String accessToken  = jwtService.generateAccessToken(userId, user.getUsername(), user.getCif(), roles);
        String refreshToken = jwtService.generateRefreshToken(userId, user.getUsername());

        // 4. Save session
        LoginSession session = LoginSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(refreshToken))
                .ipAddress(request.getClientIp())
                .deviceInfo(request.getDeviceInfo())
                .expiresAt(Instant.now().plusMillis(jwtService.getAccessExpirationMs() * 24))
                .isRevoked(false)
                .build();
        sessionRepository.save(session);

        // 5. Update last login time
        userRepository.updateLastLoginAt(user.getUserId(), Instant.now());

        log.info("Login successful for user: {} | roles: {}", user.getUsername(), roles);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .userId(userId)
                .username(user.getUsername())
                .cif(user.getCif())
                .roles(roles)
                .build();
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.debug("Refresh token request");

        // 1. Validate token signature and expiry
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. Check session
        String tokenHash = hashToken(refreshToken);
        LoginSession session = sessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID,
                        "Session not found for refresh token"));

        if (!session.isValid()) {
            throw new BusinessException(ErrorCode.SESSION_REVOKED, "Session is revoked or expired");
        }

        // 3. Load user
        User user = userRepository.findByUsernameWithRoles(
                jwtService.extractUsername(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toList());

        // 4. Issue new tokens (token rotation)
        String userId = user.getUserId().toString();
        String newAccessToken  = jwtService.generateAccessToken(userId, user.getUsername(), user.getCif(), roles);
        String newRefreshToken = jwtService.generateRefreshToken(userId, user.getUsername());

        // 5. Revoke old session, create new one
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

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .userId(userId)
                .username(user.getUsername())
                .cif(user.getCif())
                .roles(roles)
                .build();
    }

    // ── VALIDATE TOKEN (internal — called by other services) ──────

    public TokenValidationResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidationResponse.invalid("TOKEN_MISSING");
        }

        Map<String, Object> result = jwtService.validateAndExtract(token);

        if (!Boolean.TRUE.equals(result.get("valid"))) {
            return TokenValidationResponse.invalid((String) result.get("reason"));
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) result.getOrDefault("roles", List.of());

        return TokenValidationResponse.builder()
                .valid(true)
                .userId((String) result.get("userId"))
                .username((String) result.get("username"))
                .cif((String) result.get("cif"))
                .roles(roles)
                .expiresAt((Long) result.get("expiresAt"))
                .build();
    }

    // ── VERIFY OTP ────────────────────────────────────────────────

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findById(UUID.fromString(request.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        OtpRequest otp = otpRepository.findLatestValidOtp(
                user.getUserId(), request.getPurpose(), Instant.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID,
                        "No valid OTP found for user and purpose"));

        // Verify OTP code hash
        boolean valid = passwordEncoder.matches(request.getOtpCode(), otp.getOtpCodeHash());
        if (!valid) {
            throw new BusinessException(ErrorCode.OTP_INVALID, "OTP code does not match");
        }

        // Mark as used
        otpRepository.markAsUsed(otp.getOtpId());
        log.info("OTP verified for user: {} purpose: {}", request.getUserId(), request.getPurpose());
        return true;
    }

    // ── LOGOUT ────────────────────────────────────────────────────

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int revoked = sessionRepository.revokeAllByUserId(user.getUserId(), Instant.now());
            log.info("Logged out user: {} | sessions revoked: {}", username, revoked);
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
