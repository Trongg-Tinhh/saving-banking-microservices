package com.saving.customer.common;

public final class Constants {

    private Constants() {}

    // ── HTTP Headers ───────────────────────────────────────────────
    public static final String CORRELATION_ID_HEADER  = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER   = "Authorization";
    public static final String BEARER_PREFIX          = "Bearer ";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    // ── JWT Claims ─────────────────────────────────────────────────
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_CIF     = "cif";
    public static final String CLAIM_ROLES   = "roles";
    public static final String CLAIM_TYPE    = "type";
    public static final String TOKEN_TYPE_ACCESS  = "ACCESS";

    // ── CIF Generation prefix ──────────────────────────────────────
    public static final String CIF_PREFIX = "CIF";

    // ── RabbitMQ ───────────────────────────────────────────────────
    public static final String CUSTOMER_EXCHANGE         = "customer.events";
    public static final String CUSTOMER_CREATED_QUEUE    = "customer.created";
    public static final String CUSTOMER_UPDATED_QUEUE    = "customer.updated";
    public static final String CUSTOMER_KYC_QUEUE        = "customer.kyc.status";

    public static final String ROUTING_CUSTOMER_CREATED  = "customer.created.event";
    public static final String ROUTING_CUSTOMER_UPDATED  = "customer.updated.event";
    public static final String ROUTING_CUSTOMER_KYC      = "customer.kyc.updated";

    // ── Status values ──────────────────────────────────────────────
    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_BLOCKED  = "BLOCKED";

    public static final String KYC_NOT_VERIFIED = "NOT_VERIFIED";
    public static final String KYC_PENDING      = "PENDING";
    public static final String KYC_VERIFIED     = "VERIFIED";
    public static final String KYC_REJECTED     = "REJECTED";

    // ── MDC keys ───────────────────────────────────────────────────
    public static final String MDC_USERNAME_KEY = "username";
}
