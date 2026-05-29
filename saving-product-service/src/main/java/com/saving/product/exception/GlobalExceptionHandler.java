package com.saving.product.exception;

import com.saving.product.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, WebRequest request) {
        int httpStatus = ex.getErrorCode().getHttpStatus().value();
        String path    = request.getDescription(false);
        if (httpStatus >= 500) {
            log.error("[EXCEPTION] {} — [{}] {} | path: {}",
                    httpStatus, ex.getErrorCode().getCode(), ex.getMessage(), path);
        } else {
            log.warn("[EXCEPTION] {} — [{}] {} | details: {} | path: {}",
                    httpStatus, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails(), path);
        }
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().getCode(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                               WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        log.warn("[EXCEPTION] 400 Bad Request — validation failed: {} | path: {}",
                details, request.getDescription(false));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                        ErrorCode.VALIDATION_FAILED.getCode(), details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("[EXCEPTION] 403 Forbidden — access denied | path: {}", request.getDescription(false));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.getDefaultMessage(),
                        ErrorCode.FORBIDDEN.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex, WebRequest request) {
        log.warn("[EXCEPTION] 401 Unauthorized | path: {}", request.getDescription(false));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED.getDefaultMessage(),
                        ErrorCode.UNAUTHORIZED.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, WebRequest request) {
        log.error("[EXCEPTION] 500 Internal Server Error — {} | path: {}",
                ex.getMessage(), request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                        ErrorCode.INTERNAL_ERROR.getCode(), null));
    }
}
