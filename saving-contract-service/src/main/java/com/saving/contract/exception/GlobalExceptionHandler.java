package com.saving.contract.exception;

import com.saving.contract.common.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        ErrorCode errorCode = ex.getErrorCode();
        int httpStatus = errorCode.getHttpStatus().value();
        String path    = request.getDescription(false);

        if (httpStatus >= 500) {
            log.error("[EXCEPTION] {} — [{}] {} | path: {}",
                    httpStatus, errorCode.getCode(), ex.getMessage(), path);
        } else {
            log.warn("[EXCEPTION] {} — [{}] {} | path: {}",
                    httpStatus, errorCode.getCode(), ex.getMessage(), path);
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(
                        ex.getMessage() != null ? ex.getMessage() : errorCode.getDefaultMessage(),
                        errorCode.getCode(),
                        null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        log.warn("[EXCEPTION] 400 Bad Request — validation failed: {} | path: {}",
                details, request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Request validation failed")
                        .data(errors)
                        .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                        .build());
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(Exception ex, WebRequest request) {
        log.warn("[EXCEPTION] 409 Conflict — optimistic lock: {} | path: {}",
                ex.getMessage(), request.getDescription(false));
        ErrorCode errorCode = ErrorCode.OPTIMISTIC_LOCK_CONFLICT;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getDefaultMessage(), errorCode.getCode(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("[EXCEPTION] 403 Forbidden — access denied | path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied",
                        ErrorCode.FORBIDDEN.getCode(), null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, WebRequest request) {
        log.warn("[EXCEPTION] 401 Unauthorized | path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Unauthorized",
                        ErrorCode.UNAUTHORIZED.getCode(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, WebRequest request) {
        log.error("[EXCEPTION] 500 Internal Server Error — {} | path: {}",
                ex.getMessage(), request.getDescription(false), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred",
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        null));
    }
}
