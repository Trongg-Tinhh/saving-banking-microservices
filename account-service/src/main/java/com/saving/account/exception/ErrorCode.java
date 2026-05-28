package com.saving.account.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Account errors
    ACCOUNT_NOT_FOUND       ("ACC_001", "Account not found",                       HttpStatus.NOT_FOUND),
    ACCOUNT_BLOCKED         ("ACC_002", "Account is blocked",                      HttpStatus.FORBIDDEN),
    ACCOUNT_CLOSED          ("ACC_003", "Account is closed",                       HttpStatus.GONE),
    ACCOUNT_TYPE_MISMATCH   ("ACC_004", "Account type does not support this operation", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_CLOSED  ("ACC_005", "Account is already closed",               HttpStatus.CONFLICT),
    ACCOUNT_LIMIT_REACHED   ("ACC_006", "Maximum accounts per CIF reached",        HttpStatus.BAD_REQUEST),
    ACCOUNT_NO_GEN_FAILED   ("ACC_007", "Failed to generate account number",       HttpStatus.INTERNAL_SERVER_ERROR),

    // Balance errors
    INSUFFICIENT_FUNDS      ("BAL_001", "Insufficient available balance",          HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_HOLD       ("BAL_002", "Insufficient held amount for debit",      HttpStatus.UNPROCESSABLE_ENTITY),
    BALANCE_NOT_FOUND       ("BAL_003", "Balance record not found for account",    HttpStatus.NOT_FOUND),
    BALANCE_CONFLICT        ("BAL_004", "Balance was modified concurrently — please retry", HttpStatus.CONFLICT),
    NON_ZERO_BALANCE        ("BAL_005", "Cannot close account with non-zero balance", HttpStatus.CONFLICT),

    // Hold errors
    HOLD_NOT_FOUND          ("HLD_001", "Hold not found for reference",            HttpStatus.NOT_FOUND),
    HOLD_ALREADY_EXISTS     ("HLD_002", "Active hold already exists for this reference", HttpStatus.CONFLICT),
    HOLD_ALREADY_RELEASED   ("HLD_003", "Hold has already been released",          HttpStatus.CONFLICT),

    // Validation errors
    VALIDATION_FAILED       ("VAL_001", "Request validation failed",               HttpStatus.BAD_REQUEST),
    INVALID_REQUEST         ("VAL_002", "Invalid request parameters",              HttpStatus.BAD_REQUEST),

    // Auth errors
    UNAUTHORIZED            ("AUTH_001", "Authentication required",                HttpStatus.UNAUTHORIZED),
    FORBIDDEN               ("AUTH_002", "Access denied",                          HttpStatus.FORBIDDEN),

    // System errors
    INTERNAL_ERROR          ("SYS_001", "Internal server error",                   HttpStatus.INTERNAL_SERVER_ERROR),
    EVENT_PUBLISH_FAILED    ("SYS_002", "Failed to publish domain event",          HttpStatus.INTERNAL_SERVER_ERROR);

    private final String     code;
    private final String     defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code           = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus     = httpStatus;
    }
}
