package com.saving.auth.exception;

import com.saving.auth.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Business Exception ────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        int httpStatus = ex.getErrorCode().getHttpStatus().value();
        String path    = request.getDescription(false);
        // 4xx → WARN, 5xx → ERROR
        if (httpStatus >= 500) {
            log.error("[EXCEPTION] {} {} — [{}] {} | path: {}",
                    httpStatus, ex.getErrorCode().getHttpStatus().getReasonPhrase(),
                    ex.getErrorCode().getCode(), ex.getMessage(), path);
        } else {
            log.warn("[EXCEPTION] {} {} — [{}] {} | details: {} | path: {}",
                    httpStatus, ex.getErrorCode().getHttpStatus().getReasonPhrase(),
                    ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails(), path);
        }
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(
                        ex.getMessage(),
                        ex.getErrorCode().getCode(),
                        ex.getDetails()));
    }

    // ── Validation Exception ──────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[EXCEPTION] 400 Bad Request — validation failed: {} | path: {}",
                details, request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Request validation failed",
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        details));
    }

    // ── Spring Security Exceptions ────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        log.warn("[EXCEPTION] 401 Unauthorized — bad credentials | path: {}",
                request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "Invalid username or password",
                        ErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedException(
            LockedException ex, WebRequest request) {
        log.warn("[EXCEPTION] 403 Locked — account locked | path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is locked", ErrorCode.ACCOUNT_LOCKED.getCode()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(
            DisabledException ex, WebRequest request) {
        log.warn("[EXCEPTION] 403 Disabled — account disabled | path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is disabled", ErrorCode.ACCOUNT_INACTIVE.getCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("[EXCEPTION] 403 Forbidden — access denied | path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", ErrorCode.ACCESS_DENIED.getCode()));
    }

    // ── Generic Exception ─────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("[EXCEPTION] 500 Internal Server Error — {} | path: {}",
                ex.getMessage(), request.getDescription(false), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Internal server error",
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ex.getMessage()));
    }
}
