package com.saving.contract.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Contract errors ──────────────────────────────────────────────────────
    CONTRACT_NOT_FOUND          ("CON_001", "Saving contract not found",                    HttpStatus.NOT_FOUND),
    CONTRACT_NOT_ACTIVE         ("CON_002", "Contract is not in ACTIVE status",             HttpStatus.UNPROCESSABLE_ENTITY),
    CONTRACT_ALREADY_CLOSED     ("CON_003", "Contract is already closed",                   HttpStatus.CONFLICT),
    CONTRACT_NOT_MATURED        ("CON_004", "Contract has not reached maturity date",        HttpStatus.UNPROCESSABLE_ENTITY),
    CONTRACT_STILL_ACTIVE       ("CON_005", "Contract is still active (use early close)",   HttpStatus.UNPROCESSABLE_ENTITY),
    CONTRACT_IN_TERMINAL_STATE  ("CON_006", "Contract is in a terminal state",              HttpStatus.CONFLICT),
    MATURITY_INSTRUCTION_EXISTS ("CON_007", "Maturity instruction already set",             HttpStatus.CONFLICT),

    // ── Amount / product validation ──────────────────────────────────────────
    AMOUNT_BELOW_MINIMUM        ("AMT_001", "Amount is below product minimum",              HttpStatus.BAD_REQUEST),
    AMOUNT_ABOVE_MAXIMUM        ("AMT_002", "Amount exceeds product maximum",               HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS          ("AMT_003", "Insufficient funds in source account",         HttpStatus.UNPROCESSABLE_ENTITY),

    // ── Downstream service errors ─────────────────────────────────────────────
    CUSTOMER_NOT_FOUND          ("EXT_001", "Customer not found",                           HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_VALID          ("EXT_002", "Customer is not eligible to open a contract",  HttpStatus.UNPROCESSABLE_ENTITY),
    CUSTOMER_SERVICE_ERROR      ("EXT_003", "Customer service returned an error",           HttpStatus.BAD_GATEWAY),
    CUSTOMER_SERVICE_UNAVAILABLE("EXT_004", "Customer service is unavailable",              HttpStatus.SERVICE_UNAVAILABLE),

    ACCOUNT_NOT_FOUND           ("EXT_011", "Source account not found",                     HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_VALID           ("EXT_012", "Source account is not eligible",               HttpStatus.UNPROCESSABLE_ENTITY),
    ACCOUNT_SERVICE_ERROR       ("EXT_013", "Account service returned an error",            HttpStatus.BAD_GATEWAY),
    ACCOUNT_SERVICE_UNAVAILABLE ("EXT_014", "Account service is unavailable",               HttpStatus.SERVICE_UNAVAILABLE),

    PRODUCT_NOT_FOUND           ("EXT_021", "Saving product or term not found",             HttpStatus.NOT_FOUND),
    PRODUCT_NOT_VALID           ("EXT_022", "Product or term is not active",                HttpStatus.UNPROCESSABLE_ENTITY),
    PRODUCT_SERVICE_ERROR       ("EXT_023", "Product service returned an error",            HttpStatus.BAD_GATEWAY),
    PRODUCT_SERVICE_UNAVAILABLE ("EXT_024", "Product service is unavailable",               HttpStatus.SERVICE_UNAVAILABLE),
    RATE_NOT_FOUND              ("EXT_025", "No effective interest rate found for open date",HttpStatus.UNPROCESSABLE_ENTITY),

    // ── Auth ──────────────────────────────────────────────────────────────────
    UNAUTHORIZED                ("AUTH_001", "Authentication required",                     HttpStatus.UNAUTHORIZED),
    FORBIDDEN                   ("AUTH_002", "Access denied",                               HttpStatus.FORBIDDEN),

    // ── Validation ────────────────────────────────────────────────────────────
    VALIDATION_FAILED           ("VAL_001",  "Request validation failed",                   HttpStatus.BAD_REQUEST),

    // ── Concurrency ───────────────────────────────────────────────────────────
    OPTIMISTIC_LOCK_CONFLICT    ("CON_099",  "Concurrent modification detected, please retry", HttpStatus.CONFLICT),

    // ── System ────────────────────────────────────────────────────────────────
    INTERNAL_ERROR              ("SYS_001",  "Internal server error",                       HttpStatus.INTERNAL_SERVER_ERROR);

    private final String     code;
    private final String     defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code           = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus     = httpStatus;
    }
}
