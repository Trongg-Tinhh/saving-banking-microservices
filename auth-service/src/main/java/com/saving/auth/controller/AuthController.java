package com.saving.auth.controller;

import com.saving.auth.common.ApiResponse;
import com.saving.auth.common.Constants;
import com.saving.auth.dto.request.CreateUserRequest;
import com.saving.auth.dto.request.LoginRequest;
import com.saving.auth.dto.request.RefreshTokenRequest;
import com.saving.auth.dto.request.VerifyOtpRequest;
import com.saving.auth.dto.response.CreateUserResponse;
import com.saving.auth.dto.response.LoginResponse;
import com.saving.auth.dto.response.TokenValidationResponse;
import com.saving.auth.dto.response.UserInfoResponse;
import com.saving.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Login, token management, OTP, session")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/v1/auth/login ───────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username/password. Returns JWT access & refresh tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        // Enrich with client info if not set by gateway
        if (!StringUtils.hasText(request.getClientIp())) {
            request.setClientIp(getClientIp(httpRequest));
        }
        if (!StringUtils.hasText(request.getDeviceInfo())) {
            request.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        }

        LoginResponse response = authService.login(request);
        log.info("Login success: user={}", response.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response, "Login successful"));
    }

    // ── POST /api/v1/auth/refresh-token ──────────────────────────

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token pair.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    // ── POST /api/v1/auth/verify-otp ─────────────────────────────

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify a 6-digit OTP code for a given purpose.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        boolean verified = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(verified, "OTP verified successfully"));
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke all active sessions for the current user.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    // ── GET /api/v1/auth/me ───────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns info of the authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserInfoResponse info = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    // ── GET /api/v1/auth/validate ─────────────────────────────────
    // Internal endpoint: called by API Gateway and other services

    @GetMapping("/validate")
    @Operation(summary = "[INTERNAL] Validate token",
            description = "Validates JWT and returns claims. Called internally by other microservices via API Gateway. No auth required for this endpoint.")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestHeader(value = Constants.AUTHORIZATION_HEADER, required = false) String authHeader) {

        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(Constants.BEARER_PREFIX)) {
            token = authHeader.substring(Constants.BEARER_PREFIX.length());
        }

        TokenValidationResponse result = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── POST /api/v1/auth/admin/users ────────────────────────────
    // Create a CUSTOMER login account and link it to an existing CIF.
    // Only TELLER or ADMIN may call this.

    @PostMapping("/admin/users")
    @PreAuthorize("hasAnyAuthority('TELLER','ADMIN')")
    @Operation(summary = "Create customer login account",
               description = "Create a CUSTOMER-role login account for an existing CIF. " +
                             "Call AFTER POST /api/v1/customers to create the CIF first.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        CreateUserResponse response = authService.createUser(request);
        log.info("Customer user created: username={}, cif={}", response.getUsername(), response.getCif());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User account created successfully"));
    }

    // ── GET /health ───────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth Service is UP", "OK"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
