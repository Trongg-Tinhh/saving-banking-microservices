package com.saving.product.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Product errors
    PRODUCT_NOT_FOUND        ("PROD_001", "Saving product not found",              HttpStatus.NOT_FOUND),
    PRODUCT_ALREADY_EXISTS   ("PROD_002", "Product code already exists",           HttpStatus.CONFLICT),
    PRODUCT_INACTIVE         ("PROD_003", "Saving product is not active",          HttpStatus.UNPROCESSABLE_ENTITY),
    PRODUCT_CODE_IMMUTABLE   ("PROD_004", "Product code cannot be changed",        HttpStatus.BAD_REQUEST),

    // Term errors
    TERM_NOT_FOUND           ("TERM_001", "Saving term not found",                 HttpStatus.NOT_FOUND),
    TERM_ALREADY_EXISTS      ("TERM_002", "Term ID already exists for this product", HttpStatus.CONFLICT),
    TERM_INACTIVE            ("TERM_003", "Saving term is not active",             HttpStatus.UNPROCESSABLE_ENTITY),

    // Rate errors
    RATE_NOT_FOUND           ("RATE_001", "No effective interest rate found for the given date", HttpStatus.NOT_FOUND),
    RATE_DATE_CONFLICT       ("RATE_002", "Rate date range conflicts with existing rates",       HttpStatus.CONFLICT),
    RATE_DATE_INVALID        ("RATE_003", "effective_to must be after effective_from",           HttpStatus.BAD_REQUEST),

    // Policy errors
    POLICY_NOT_FOUND         ("POLY_001", "Early withdrawal policy not found",     HttpStatus.NOT_FOUND),

    // Amount validation
    AMOUNT_BELOW_MINIMUM     ("AMT_001",  "Amount is below product minimum",       HttpStatus.BAD_REQUEST),
    AMOUNT_ABOVE_MAXIMUM     ("AMT_002",  "Amount exceeds product maximum",        HttpStatus.BAD_REQUEST),

    // Auth
    UNAUTHORIZED             ("AUTH_001", "Authentication required",               HttpStatus.UNAUTHORIZED),
    FORBIDDEN                ("AUTH_002", "Access denied",                         HttpStatus.FORBIDDEN),

    // Validation
    VALIDATION_FAILED        ("VAL_001",  "Request validation failed",             HttpStatus.BAD_REQUEST),

    // System
    INTERNAL_ERROR           ("SYS_001",  "Internal server error",                 HttpStatus.INTERNAL_SERVER_ERROR);

    private final String     code;
    private final String     defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code           = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus     = httpStatus;
    }
}
