package com.saving.auth.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Standard API Response wrapper for all endpoints.
 *
 * Success:
 * { "success": true, "message": "...", "data": {...}, "error": null,
 *   "timestamp": "...", "correlationId": "..." }
 *
 * Error:
 * { "success": false, "message": "...", "data": null, "error": {...},
 *   "timestamp": "...", "correlationId": "..." }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorDetail error;
    private final String timestamp;
    private final String correlationId;

    // ── Factories ────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now().toString())
                .correlationId(getCorrelationId())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .correlationId(getCorrelationId())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.of(errorCode, details))
                .timestamp(Instant.now().toString())
                .correlationId(getCorrelationId())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.of(errorCode, null))
                .timestamp(Instant.now().toString())
                .correlationId(getCorrelationId())
                .build();
    }

    private static String getCorrelationId() {
        String corr = MDC.get(Constants.CORRELATION_ID_KEY);
        return corr != null ? corr : "NO_CORR";
    }

    // ── Inner class ──────────────────────────────────────────────

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private final String code;
        private final String details;

        public static ErrorDetail of(String code, String details) {
            return ErrorDetail.builder().code(code).details(details).build();
        }
    }
}
