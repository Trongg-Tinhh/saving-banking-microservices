package com.saving.transaction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    TRANSACTION_NOT_FOUND       ("TRX_001", "Transaction not found",                    HttpStatus.NOT_FOUND),
    DUPLICATE_TRANSACTION_REF   ("TRX_002", "Transaction reference already exists",     HttpStatus.CONFLICT),

    CBS_SYNC_FAILED             ("CBS_001", "Failed to sync transaction to CBS",        HttpStatus.BAD_GATEWAY),
    CBS_UNAVAILABLE             ("CBS_002", "Core banking system is unavailable",       HttpStatus.SERVICE_UNAVAILABLE),

    UNAUTHORIZED                ("AUTH_001", "Authentication required",                 HttpStatus.UNAUTHORIZED),
    FORBIDDEN                   ("AUTH_002", "Access denied",                           HttpStatus.FORBIDDEN),

    VALIDATION_FAILED           ("VAL_001",  "Request validation failed",               HttpStatus.BAD_REQUEST),

    INTERNAL_ERROR              ("SYS_001",  "Internal server error",                   HttpStatus.INTERNAL_SERVER_ERROR);

    private final String     code;
    private final String     defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code           = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus     = httpStatus;
    }
}
