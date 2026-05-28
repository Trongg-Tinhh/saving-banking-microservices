package com.saving.contract.common;

public final class Constants {

    private Constants() {}

    public static final String CORRELATION_ID_HEADER   = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER    = "Authorization";
    public static final String BEARER_PREFIX           = "Bearer ";
    public static final String CORRELATION_ID_MDC_KEY  = "correlationId";

    public static final String CLAIM_ROLE   = "role";
    public static final String CLAIM_TYPE   = "type";
    public static final String CLAIM_CIF    = "cif";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    // ── Contract statuses ─────────────────────────────────────────────────
    public static final class ContractStatus {
        private ContractStatus() {}

        public static final String PENDING      = "PENDING";
        public static final String ACTIVE       = "ACTIVE";
        public static final String MATURED      = "MATURED";
        public static final String CLOSED       = "CLOSED";
        public static final String EARLY_CLOSED = "EARLY_CLOSED";
        public static final String CANCELLED    = "CANCELLED";
        public static final String FAILED       = "FAILED";
    }

    // ── Close types ───────────────────────────────────────────────────────
    public static final class CloseType {
        private CloseType() {}

        public static final String MATURITY          = "MATURITY";
        public static final String EARLY_WITHDRAWAL  = "EARLY_WITHDRAWAL";
    }

    // ── Maturity instruction types ────────────────────────────────────────
    public static final class MaturityInstructionType {
        private MaturityInstructionType() {}

        public static final String TRANSFER_PRINCIPAL_AND_INTEREST = "TRANSFER_PRINCIPAL_AND_INTEREST";
        public static final String RENEW_PRINCIPAL                 = "RENEW_PRINCIPAL";
        public static final String RENEW_PRINCIPAL_AND_INTEREST    = "RENEW_PRINCIPAL_AND_INTEREST";
    }

    // ── Interest payment methods ──────────────────────────────────────────
    public static final class InterestPaymentMethod {
        private InterestPaymentMethod() {}

        public static final String END_OF_TERM = "END_OF_TERM";
        public static final String MONTHLY     = "MONTHLY";
        public static final String QUARTERLY   = "QUARTERLY";
    }

    // ── RabbitMQ ──────────────────────────────────────────────────────────
    public static final class Rabbit {
        private Rabbit() {}

        public static final String CONTRACT_EXCHANGE        = "saving.contract.events";

        // Queues
        public static final String CONTRACT_OPENED_QUEUE   = "saving.contract.opened";
        public static final String CONTRACT_CLOSED_QUEUE   = "saving.contract.closed";
        public static final String CONTRACT_MATURED_QUEUE  = "saving.contract.matured";

        // Routing keys
        public static final String CONTRACT_OPENED_KEY     = "saving.contract.opened.event";
        public static final String CONTRACT_CLOSED_KEY     = "saving.contract.closed.event";
        public static final String CONTRACT_MATURED_KEY    = "saving.contract.matured.event";

        // Dead-letter
        public static final String CONTRACT_DLX            = "saving.contract.events.dlx";
        public static final String CONTRACT_DLQ            = "saving.contract.events.dlq";
    }

    // ── Contract number ───────────────────────────────────────────────────
    public static final String CONTRACT_PREFIX = "SC";
}
