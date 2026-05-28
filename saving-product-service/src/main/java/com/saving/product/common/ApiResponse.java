package com.saving.product.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String  message;
    private T       data;
    private String  error;
    private String  errorCode;
    private String  details;
    private String  timestamp;
    private String  correlationId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true).data(data)
                .timestamp(Instant.now().toString())
                .correlationId(MDC.get(Constants.CORRELATION_ID_MDC_KEY))
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .timestamp(Instant.now().toString())
                .correlationId(MDC.get(Constants.CORRELATION_ID_MDC_KEY))
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, String details) {
        return ApiResponse.<T>builder()
                .success(false).message(message).error(message)
                .errorCode(errorCode).details(details)
                .timestamp(Instant.now().toString())
                .correlationId(MDC.get(Constants.CORRELATION_ID_MDC_KEY))
                .build();
    }
}
