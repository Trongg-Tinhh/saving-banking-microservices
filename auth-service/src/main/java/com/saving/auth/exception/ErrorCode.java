package com.saving.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Auth errors ───────────────────────────────────────────────
    INVALID_CREDENTIALS   ("AUTH_001", "Invalid username or password",         HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED        ("AUTH_002", "Account is locked",                    HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE      ("AUTH_003", "Account is inactive",                  HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED         ("AUTH_004", "Token has expired",                    HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID         ("AUTH_005", "Token is invalid or malformed",        HttpStatus.UNAUTHORIZED),
    TOKEN_MISSING         ("AUTH_006", "Authorization token is missing",       HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID ("AUTH_007", "Refresh token is invalid or expired",  HttpStatus.UNAUTHORIZED),
    SESSION_REVOKED       ("AUTH_008", "Session has been revoked",             HttpStatus.UNAUTHORIZED),
    OTP_INVALID           ("AUTH_009", "OTP code is invalid or expired",       HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED      ("AUTH_010", "OTP has already been used",            HttpStatus.BAD_REQUEST),
    ACCESS_DENIED         ("AUTH_011", "Access denied — insufficient role",    HttpStatus.FORBIDDEN),

    // ── User errors ───────────────────────────────────────────────
    USER_NOT_FOUND        ("USER_001", "User not found",                       HttpStatus.NOT_FOUND),
    USERNAME_TAKEN        ("USER_002", "Username already exists",              HttpStatus.CONFLICT),

    // ── Validation errors ─────────────────────────────────────────
    VALIDATION_FAILED     ("VAL_001",  "Request validation failed",            HttpStatus.BAD_REQUEST),
    INVALID_REQUEST       ("VAL_002",  "Invalid request parameters",           HttpStatus.BAD_REQUEST),

    // ── System errors ─────────────────────────────────────────────
    INTERNAL_ERROR        ("SYS_001",  "Internal server error",                HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE   ("SYS_002",  "Service temporarily unavailable",      HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
