package com.saving.auth.common;

public final class Constants {

    private Constants() {}

    // ── HTTP Headers ──────────────────────────────────────────────
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER  = "Authorization";
    public static final String BEARER_PREFIX          = "Bearer ";

    // ── MDC Keys ──────────────────────────────────────────────────
    public static final String CORRELATION_ID_KEY = "correlationId";

    // ── RabbitMQ ──────────────────────────────────────────────────
    public static final String AUTH_EXCHANGE       = "auth.events";
    public static final String AUTH_LOGIN_QUEUE    = "auth.login";
    public static final String AUTH_LOGOUT_QUEUE   = "auth.logout";

    // ── OTP ───────────────────────────────────────────────────────
    public static final int    OTP_EXPIRY_MINUTES  = 5;
    public static final int    OTP_LENGTH           = 6;

    // ── Token ─────────────────────────────────────────────────────
    public static final String TOKEN_TYPE_ACCESS   = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH  = "REFRESH";

    // ── Claims ────────────────────────────────────────────────────
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_CIF     = "cif";
    public static final String CLAIM_ROLES   = "roles";
    public static final String CLAIM_TYPE    = "type";
}
