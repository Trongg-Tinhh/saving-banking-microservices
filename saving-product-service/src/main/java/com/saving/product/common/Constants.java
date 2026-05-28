package com.saving.product.common;

public final class Constants {

    private Constants() {}

    public static final String CORRELATION_ID_HEADER  = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER   = "Authorization";
    public static final String BEARER_PREFIX          = "Bearer ";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_CIF     = "cif";
    public static final String CLAIM_ROLES   = "roles";
    public static final String CLAIM_TYPE    = "type";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    // Interest payment methods
    public static final String IPM_END_OF_TERM = "END_OF_TERM";
    public static final String IPM_MONTHLY     = "MONTHLY";
    public static final String IPM_QUARTERLY   = "QUARTERLY";
    public static final String IPM_UPFRONT     = "UPFRONT";
}
