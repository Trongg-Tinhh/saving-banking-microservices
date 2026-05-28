package com.saving.customer.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Customer errors
    CUSTOMER_NOT_FOUND   ("CUST_001", "Customer not found",                    HttpStatus.NOT_FOUND),
    CUSTOMER_ALREADY_EXISTS("CUST_002","Customer with this ID number already exists", HttpStatus.CONFLICT),
    CUSTOMER_BLOCKED     ("CUST_003", "Customer account is blocked",            HttpStatus.FORBIDDEN),
    CUSTOMER_INACTIVE    ("CUST_004", "Customer account is inactive",           HttpStatus.FORBIDDEN),
    CIF_GENERATION_FAILED("CUST_005", "Failed to generate CIF number",          HttpStatus.INTERNAL_SERVER_ERROR),

    // KYC errors
    KYC_NOT_FOUND        ("KYC_001",  "KYC record not found for customer",      HttpStatus.NOT_FOUND),
    KYC_ALREADY_VERIFIED ("KYC_002",  "Customer KYC is already verified",       HttpStatus.CONFLICT),
    KYC_NOT_VERIFIED     ("KYC_003",  "Customer KYC is not verified",           HttpStatus.UNPROCESSABLE_ENTITY),
    KYC_REJECTED         ("KYC_004",  "Customer KYC has been rejected",         HttpStatus.UNPROCESSABLE_ENTITY),
    KYC_REJECTION_REASON_REQUIRED("KYC_005","Rejection reason is required when rejecting KYC", HttpStatus.BAD_REQUEST),

    // Contact errors
    CONTACT_NOT_FOUND    ("CONT_001", "Contact not found",                      HttpStatus.NOT_FOUND),
    CONTACT_LIMIT_REACHED("CONT_002", "Maximum contact limit (5) reached",      HttpStatus.BAD_REQUEST),

    // Validation errors
    VALIDATION_FAILED    ("VAL_001",  "Request validation failed",              HttpStatus.BAD_REQUEST),
    INVALID_REQUEST      ("VAL_002",  "Invalid request parameters",             HttpStatus.BAD_REQUEST),

    // Auth errors
    UNAUTHORIZED         ("AUTH_001", "Authentication required",                HttpStatus.UNAUTHORIZED),
    FORBIDDEN            ("AUTH_002", "Access denied",                          HttpStatus.FORBIDDEN),

    // System errors
    INTERNAL_ERROR       ("SYS_001",  "Internal server error",                  HttpStatus.INTERNAL_SERVER_ERROR),
    EVENT_PUBLISH_FAILED ("SYS_002",  "Failed to publish domain event",         HttpStatus.INTERNAL_SERVER_ERROR);

    private final String     code;
    private final String     defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code           = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus     = httpStatus;
    }
}
