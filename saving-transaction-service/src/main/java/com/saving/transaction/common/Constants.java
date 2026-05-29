package com.saving.transaction.common;

public final class Constants {

    private Constants() {}

    public static final String CORRELATION_ID_HEADER  = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER   = "Authorization";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    // ── Transaction types ─────────────────────────────────────────────────
    public static final class TxType {
        private TxType() {}
        public static final String DEBIT    = "DEBIT";
        public static final String CREDIT   = "CREDIT";
        public static final String INTEREST = "INTEREST";
    }

    // ── Transaction statuses ──────────────────────────────────────────────
    public static final class TxStatus {
        private TxStatus() {}
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED    = "FAILED";
    }

    // ── CBS sync statuses ─────────────────────────────────────────────────
    public static final class CbsStatus {
        private CbsStatus() {}
        public static final String PENDING = "PENDING";
        public static final String SYNCED  = "SYNCED";
        public static final String FAILED  = "FAILED";
    }

    // ── MDC keys ──────────────────────────────────────────────────────────
    public static final String MDC_USERNAME_KEY = "username";

    // ── RabbitMQ — Consuming from contract-service ────────────────────────
    public static final class Rabbit {
        private Rabbit() {}

        // Exchanges to consume from
        public static final String CONTRACT_EXCHANGE = "saving.contract.events";

        // Queues this service binds
        public static final String TX_CONTRACT_OPENED_QUEUE  = "tx.contract.opened";
        public static final String TX_CONTRACT_CLOSED_QUEUE  = "tx.contract.closed";
        public static final String TX_CONTRACT_MATURED_QUEUE = "tx.contract.matured";

        // Routing keys (must match contract-service routing keys)
        public static final String CONTRACT_OPENED_KEY  = "saving.contract.opened.event";
        public static final String CONTRACT_CLOSED_KEY  = "saving.contract.closed.event";
        public static final String CONTRACT_MATURED_KEY = "saving.contract.matured.event";

        // Dead-letter
        public static final String TX_DLX = "saving.transaction.dlx";
        public static final String TX_DLQ = "saving.transaction.dlq";
    }
}
