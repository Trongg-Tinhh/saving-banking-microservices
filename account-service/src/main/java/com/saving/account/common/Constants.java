package com.saving.account.common;

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
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    // ── Account types & statuses ───────────────────────────────────
    public static final String ACCOUNT_TYPE_PAYMENT = "PAYMENT";
    public static final String ACCOUNT_TYPE_SAVING  = "SAVING";
    public static final String ACCOUNT_TYPE_LOAN    = "LOAN";

    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_CLOSED  = "CLOSED";

    // ── Hold statuses ──────────────────────────────────────────────
    public static final String HOLD_ACTIVE    = "ACTIVE";
    public static final String HOLD_RELEASED  = "RELEASED";
    public static final String HOLD_CANCELLED = "CANCELLED";

    // ── RabbitMQ ───────────────────────────────────────────────────
    public static final String ACCOUNT_EXCHANGE         = "account.events";
    public static final String ACCOUNT_CREATED_QUEUE   = "account.created";
    public static final String ACCOUNT_DEBITED_QUEUE   = "account.debited";
    public static final String ACCOUNT_CREDITED_QUEUE  = "account.credited";
    public static final String ACCOUNT_STATUS_QUEUE    = "account.status.changed";

    public static final String ROUTING_ACCOUNT_CREATED = "account.created.event";
    public static final String ROUTING_ACCOUNT_DEBIT   = "account.debit.event";
    public static final String ROUTING_ACCOUNT_CREDIT  = "account.credit.event";
    public static final String ROUTING_ACCOUNT_STATUS  = "account.status.event";

    // ── Account number prefix ──────────────────────────────────────
    public static final String ACCOUNT_PREFIX = "ACC";
}
