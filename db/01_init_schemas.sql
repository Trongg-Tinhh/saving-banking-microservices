-- ==============================================================
-- SAVING BANKING SYSTEM - DATABASE INITIALIZATION
-- Database: saving_banking
-- ==============================================================
-- Run order: 01_init_schemas.sql → 02_seed_data.sql
-- ==============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================
-- CREATE SCHEMAS
-- ==============================================================
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS customer_schema;
CREATE SCHEMA IF NOT EXISTS account_schema;
CREATE SCHEMA IF NOT EXISTS saving_product_schema;
CREATE SCHEMA IF NOT EXISTS saving_contract_schema;
CREATE SCHEMA IF NOT EXISTS transaction_schema;
CREATE SCHEMA IF NOT EXISTS interest_schema;
CREATE SCHEMA IF NOT EXISTS saving_lifecycle_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- ==============================================================
-- AUTH_SCHEMA
-- ==============================================================

CREATE TABLE auth_schema.users (
    user_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username       VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    cif            VARCHAR(20),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at  TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'INACTIVE'))
);

CREATE TABLE auth_schema.roles (
    role_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code   VARCHAR(50) NOT NULL,
    role_name   VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_code UNIQUE (role_code)
);

CREATE TABLE auth_schema.permissions (
    permission_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_code VARCHAR(100) NOT NULL,
    resource        VARCHAR(100) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permissions_code UNIQUE (permission_code)
);

CREATE TABLE auth_schema.user_roles (
    user_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES auth_schema.users(user_id) ON DELETE CASCADE,
    role_id      UUID NOT NULL REFERENCES auth_schema.roles(role_id) ON DELETE CASCADE,
    assigned_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id)
);

CREATE TABLE auth_schema.otp_requests (
    otp_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES auth_schema.users(user_id) ON DELETE CASCADE,
    otp_code_hash VARCHAR(255) NOT NULL,
    purpose       VARCHAR(50) NOT NULL,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('LOGIN', 'RESET_PASSWORD', 'TRANSACTION', 'VERIFY_ACCOUNT'))
);

CREATE TABLE auth_schema.login_sessions (
    session_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth_schema.users(user_id) ON DELETE CASCADE,
    refresh_token_hash  VARCHAR(255),
    ip_address          VARCHAR(50),
    device_info         TEXT,
    user_agent          TEXT,
    expires_at          TIMESTAMP WITH TIME ZONE,
    is_revoked          BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for auth_schema
CREATE INDEX idx_auth_users_cif ON auth_schema.users(cif);
CREATE INDEX idx_auth_users_status ON auth_schema.users(status);
CREATE INDEX idx_auth_sessions_user_id ON auth_schema.login_sessions(user_id);
CREATE INDEX idx_auth_sessions_revoked ON auth_schema.login_sessions(is_revoked);
CREATE INDEX idx_auth_otp_user_id ON auth_schema.otp_requests(user_id);
CREATE INDEX idx_auth_otp_purpose ON auth_schema.otp_requests(purpose, is_used);

-- ==============================================================
-- CUSTOMER_SCHEMA
-- ==============================================================

CREATE TABLE customer_schema.customers (
    cif           VARCHAR(20) PRIMARY KEY,
    full_name     VARCHAR(200) NOT NULL,
    date_of_birth DATE,
    gender        VARCHAR(10),
    nationality   VARCHAR(10) NOT NULL DEFAULT 'VN',
    id_number     VARCHAR(50),
    id_type       VARCHAR(30),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    CONSTRAINT chk_customers_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER') OR gender IS NULL)
);

CREATE TABLE customer_schema.customer_kyc (
    kyc_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif              VARCHAR(20) NOT NULL,
    kyc_status       VARCHAR(20) NOT NULL DEFAULT 'NOT_VERIFIED',
    verified_at      TIMESTAMP WITH TIME ZONE,
    verified_by      VARCHAR(100),
    rejection_reason TEXT,
    doc_type         VARCHAR(50),
    doc_url          TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customer_kyc_cif UNIQUE (cif),
    CONSTRAINT chk_kyc_status CHECK (kyc_status IN ('NOT_VERIFIED', 'PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE customer_schema.customer_contacts (
    contact_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif          VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20),
    email        VARCHAR(100),
    address      TEXT,
    district     VARCHAR(100),
    city         VARCHAR(100),
    is_primary   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for customer_schema
CREATE INDEX idx_customers_status ON customer_schema.customers(status);
CREATE INDEX idx_customers_id_number ON customer_schema.customers(id_number);
CREATE INDEX idx_customer_kyc_cif ON customer_schema.customer_kyc(cif);
CREATE INDEX idx_customer_kyc_status ON customer_schema.customer_kyc(kyc_status);
CREATE INDEX idx_customer_contacts_cif ON customer_schema.customer_contacts(cif);

-- ==============================================================
-- ACCOUNT_SCHEMA
-- ==============================================================

CREATE TABLE account_schema.accounts (
    account_no   VARCHAR(20) PRIMARY KEY,
    cif          VARCHAR(20) NOT NULL,
    account_type VARCHAR(30) NOT NULL DEFAULT 'PAYMENT',
    currency     VARCHAR(10) NOT NULL DEFAULT 'VND',
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    open_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    branch_code  VARCHAR(20),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('PAYMENT', 'SAVING', 'LOAN'))
);

CREATE TABLE account_schema.account_balances (
    balance_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_no         VARCHAR(20) NOT NULL,
    available_balance  NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ledger_balance     NUMERIC(20, 2) NOT NULL DEFAULT 0,
    hold_amount        NUMERIC(20, 2) NOT NULL DEFAULT 0,
    currency           VARCHAR(10) NOT NULL DEFAULT 'VND',
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_account_balances_account_no UNIQUE (account_no),
    CONSTRAINT chk_balance_available CHECK (available_balance >= 0),
    CONSTRAINT chk_balance_hold CHECK (hold_amount >= 0)
);

CREATE TABLE account_schema.account_hold_logs (
    hold_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_no   VARCHAR(20) NOT NULL,
    hold_amount  NUMERIC(20, 2) NOT NULL,
    hold_reason  TEXT,
    hold_ref     VARCHAR(100),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    released_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_hold_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CANCELLED'))
);

-- Indexes for account_schema
CREATE INDEX idx_accounts_cif ON account_schema.accounts(cif);
CREATE INDEX idx_accounts_status ON account_schema.accounts(status);
CREATE INDEX idx_account_balances_account_no ON account_schema.account_balances(account_no);
CREATE INDEX idx_account_hold_logs_account_no ON account_schema.account_hold_logs(account_no);
CREATE INDEX idx_account_hold_logs_status ON account_schema.account_hold_logs(status);

-- ==============================================================
-- SAVING_PRODUCT_SCHEMA
-- ==============================================================

CREATE TABLE saving_product_schema.saving_products (
    product_code            VARCHAR(50) PRIMARY KEY,
    product_name            VARCHAR(200) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'VND',
    min_amount              NUMERIC(20, 2),
    max_amount              NUMERIC(20, 2),
    interest_payment_method VARCHAR(30) NOT NULL DEFAULT 'END_OF_TERM',
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    description             TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_payment_method CHECK (
        interest_payment_method IN ('END_OF_TERM', 'MONTHLY', 'QUARTERLY', 'UPFRONT')
    )
);

CREATE TABLE saving_product_schema.saving_terms (
    term_id       VARCHAR(50) PRIMARY KEY,
    product_code  VARCHAR(50) NOT NULL REFERENCES saving_product_schema.saving_products(product_code),
    term_months   INTEGER,
    term_days     INTEGER,
    term_label    VARCHAR(50) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE saving_product_schema.interest_rate_configs (
    rate_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code   VARCHAR(50) NOT NULL REFERENCES saving_product_schema.saving_products(product_code),
    term_id        VARCHAR(50) NOT NULL REFERENCES saving_product_schema.saving_terms(term_id),
    annual_rate    NUMERIC(10, 4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to   DATE,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rate_positive CHECK (annual_rate >= 0),
    CONSTRAINT chk_rate_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE TABLE saving_product_schema.early_withdrawal_policies (
    policy_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code   VARCHAR(50) NOT NULL REFERENCES saving_product_schema.saving_products(product_code),
    min_days_held  INTEGER NOT NULL DEFAULT 0,
    penalty_rate   NUMERIC(10, 4) NOT NULL DEFAULT 0,
    use_demand_rate BOOLEAN NOT NULL DEFAULT TRUE,
    demand_rate    NUMERIC(10, 4) DEFAULT 0.5,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for saving_product_schema
CREATE INDEX idx_saving_terms_product_code ON saving_product_schema.saving_terms(product_code);
CREATE INDEX idx_interest_rate_product_term ON saving_product_schema.interest_rate_configs(product_code, term_id);
CREATE INDEX idx_interest_rate_active ON saving_product_schema.interest_rate_configs(is_active, effective_from, effective_to);
CREATE INDEX idx_early_withdrawal_product ON saving_product_schema.early_withdrawal_policies(product_code);

-- ==============================================================
-- SAVING_CONTRACT_SCHEMA
-- ==============================================================

CREATE TABLE saving_contract_schema.saving_contracts (
    contract_no             VARCHAR(50) PRIMARY KEY,
    cif                     VARCHAR(20) NOT NULL,
    product_code            VARCHAR(50) NOT NULL,
    term_id                 VARCHAR(50) NOT NULL,
    principal_amount        NUMERIC(20, 2) NOT NULL,
    interest_rate           NUMERIC(10, 4) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'VND',
    open_date               DATE NOT NULL,
    maturity_date           DATE NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    interest_payment_method VARCHAR(30) NOT NULL,
    source_account_no       VARCHAR(20) NOT NULL,
    branch_code             VARCHAR(20),
    opened_by               VARCHAR(100),
    closed_at               TIMESTAMP WITH TIME ZONE,
    close_type              VARCHAR(30),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_contract_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'MATURED', 'CLOSED', 'EARLY_CLOSED', 'CANCELLED', 'FAILED')
    ),
    CONSTRAINT chk_contract_amount CHECK (principal_amount > 0),
    CONSTRAINT chk_contract_dates CHECK (maturity_date > open_date)
);

CREATE TABLE saving_contract_schema.maturity_instructions (
    instruction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no         VARCHAR(50) NOT NULL,
    instruction_type    VARCHAR(50) NOT NULL,
    new_term_id         VARCHAR(50),
    receiving_account_no VARCHAR(20),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_maturity_instruction_contract UNIQUE (contract_no),
    CONSTRAINT chk_instruction_type CHECK (
        instruction_type IN (
            'TRANSFER_PRINCIPAL_AND_INTEREST',
            'RENEW_PRINCIPAL',
            'RENEW_PRINCIPAL_AND_INTEREST'
        )
    )
);

CREATE TABLE saving_contract_schema.saving_contract_status_history (
    history_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no  VARCHAR(50) NOT NULL,
    from_status  VARCHAR(30),
    to_status    VARCHAR(30) NOT NULL,
    changed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    changed_by   VARCHAR(100),
    reason       TEXT,
    correlation_id VARCHAR(100)
);

-- Indexes for saving_contract_schema
CREATE INDEX idx_saving_contracts_cif ON saving_contract_schema.saving_contracts(cif);
CREATE INDEX idx_saving_contracts_status ON saving_contract_schema.saving_contracts(status);
CREATE INDEX idx_saving_contracts_maturity ON saving_contract_schema.saving_contracts(maturity_date, status);
CREATE INDEX idx_saving_contracts_product ON saving_contract_schema.saving_contracts(product_code);
CREATE INDEX idx_maturity_instructions_contract ON saving_contract_schema.maturity_instructions(contract_no);
CREATE INDEX idx_contract_status_history ON saving_contract_schema.saving_contract_status_history(contract_no);

-- ==============================================================
-- TRANSACTION_SCHEMA
-- ==============================================================

CREATE TABLE transaction_schema.transactions (
    transaction_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type       VARCHAR(50) NOT NULL,
    contract_no            VARCHAR(50),
    cif                    VARCHAR(20),
    amount                 NUMERIC(20, 2) NOT NULL,
    currency               VARCHAR(10) NOT NULL DEFAULT 'VND',
    account_no             VARCHAR(20),
    destination_account_no VARCHAR(20),
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref        VARCHAR(200),
    correlation_id         VARCHAR(100),
    cbs_reference          VARCHAR(100),
    description            TEXT,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                BIGINT NOT NULL DEFAULT 0,
    cbs_sync_status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cbs_sync_attempts      INT NOT NULL DEFAULT 0,
    cbs_sync_error         VARCHAR(1000),
    cbs_synced_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_transactions_ref UNIQUE (transaction_ref),
    CONSTRAINT chk_tx_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_tx_type CHECK (
        transaction_type IN (
            'OPEN_SAVING', 'CLOSE_SAVING', 'EARLY_CLOSE_SAVING',
            'INTEREST_PAYMENT', 'MATURITY_PAYMENT', 'RENEWAL', 'REVERSAL'
        )
    ),
    CONSTRAINT chk_tx_amount CHECK (amount > 0)
);

CREATE TABLE transaction_schema.ledger_entries (
    entry_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_id          UUID NOT NULL REFERENCES transaction_schema.transactions(transaction_id),
    debit_account  VARCHAR(20),
    credit_account VARCHAR(20),
    amount         NUMERIC(20, 2) NOT NULL,
    currency       VARCHAR(10) NOT NULL DEFAULT 'VND',
    entry_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    description    TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_entry_amount CHECK (amount > 0)
);

CREATE TABLE transaction_schema.core_banking_sync_logs (
    sync_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_id            UUID NOT NULL REFERENCES transaction_schema.transactions(transaction_id),
    cbs_ref          VARCHAR(100),
    cbs_status       VARCHAR(20),
    request_payload  TEXT,
    response_payload TEXT,
    synced_at        TIMESTAMP WITH TIME ZONE,
    retry_count      INTEGER NOT NULL DEFAULT 0,
    error_message    TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction_schema.outbox_events (
    outbox_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT NOT NULL,
    is_published   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMP WITH TIME ZONE,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    last_error     TEXT
);

-- Indexes for transaction_schema
CREATE INDEX idx_transactions_contract_no ON transaction_schema.transactions(contract_no);
CREATE INDEX idx_transactions_cif ON transaction_schema.transactions(cif);
CREATE INDEX idx_transactions_status ON transaction_schema.transactions(status);
CREATE INDEX idx_transactions_tx_type ON transaction_schema.transactions(transaction_type);
CREATE INDEX idx_transactions_created_at ON transaction_schema.transactions(created_at);
CREATE INDEX idx_ledger_tx_id ON transaction_schema.ledger_entries(tx_id);
CREATE INDEX idx_ledger_entry_date ON transaction_schema.ledger_entries(entry_date);
CREATE INDEX idx_cbs_sync_tx_id ON transaction_schema.core_banking_sync_logs(tx_id);
CREATE INDEX idx_outbox_published ON transaction_schema.outbox_events(is_published, created_at);
CREATE INDEX idx_outbox_event_type ON transaction_schema.outbox_events(event_type);

-- ==============================================================
-- INTEREST_SCHEMA
-- ==============================================================

CREATE TABLE interest_schema.interest_calculations (
    calc_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no     VARCHAR(50) NOT NULL,
    calc_type       VARCHAR(30) NOT NULL,
    principal       NUMERIC(20, 2) NOT NULL,
    annual_rate     NUMERIC(10, 4) NOT NULL,
    from_date       DATE NOT NULL,
    to_date         DATE NOT NULL,
    days            INTEGER NOT NULL,
    interest_amount NUMERIC(20, 2) NOT NULL,
    calc_formula    VARCHAR(50) NOT NULL DEFAULT 'SIMPLE',
    penalty_rate    NUMERIC(10, 4),
    penalty_amount  NUMERIC(20, 2),
    calc_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_calc_type CHECK (
        calc_type IN ('EXPECTED', 'MATURITY', 'EARLY_CLOSING', 'PERIODIC')
    )
);

CREATE TABLE interest_schema.interest_schedules (
    schedule_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no       VARCHAR(50) NOT NULL,
    payment_date      DATE NOT NULL,
    expected_interest NUMERIC(20, 2) NOT NULL,
    actual_interest   NUMERIC(20, 2),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at           TIMESTAMP WITH TIME ZONE,
    tx_id             VARCHAR(100),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_schedule_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'SKIPPED'))
);

CREATE TABLE interest_schema.interest_payment_history (
    payment_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no   VARCHAR(50) NOT NULL,
    tx_id         VARCHAR(100),
    amount        NUMERIC(20, 2) NOT NULL,
    payment_date  DATE NOT NULL,
    payment_type  VARCHAR(30),
    notes         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for interest_schema
CREATE INDEX idx_interest_calc_contract ON interest_schema.interest_calculations(contract_no);
CREATE INDEX idx_interest_calc_type ON interest_schema.interest_calculations(calc_type);
CREATE INDEX idx_interest_schedules_contract ON interest_schema.interest_schedules(contract_no);
CREATE INDEX idx_interest_schedules_payment_date ON interest_schema.interest_schedules(payment_date, status);
CREATE INDEX idx_interest_payment_history_contract ON interest_schema.interest_payment_history(contract_no);

-- ==============================================================
-- SAVING_LIFECYCLE_SCHEMA
-- ==============================================================

CREATE TABLE saving_lifecycle_schema.lifecycle_jobs (
    job_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type         VARCHAR(50) NOT NULL,
    scan_date        DATE,
    triggered_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status           VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    total_contracts  INTEGER NOT NULL DEFAULT 0,
    processed        INTEGER NOT NULL DEFAULT 0,
    failed           INTEGER NOT NULL DEFAULT 0,
    skipped          INTEGER NOT NULL DEFAULT 0,
    completed_at     TIMESTAMP WITH TIME ZONE,
    error_message    TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_job_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL')),
    CONSTRAINT chk_job_type CHECK (
        job_type IN ('MATURITY_SCAN', 'INTEREST_PAYMENT_JOB', 'STATUS_SYNC_JOB')
    )
);

CREATE TABLE saving_lifecycle_schema.maturity_processing_logs (
    log_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id           UUID NOT NULL REFERENCES saving_lifecycle_schema.lifecycle_jobs(job_id),
    contract_no      VARCHAR(50) NOT NULL,
    instruction_type VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at     TIMESTAMP WITH TIME ZONE,
    error_message    TEXT,
    retry_count      INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_maturity_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE TABLE saving_lifecycle_schema.renewal_processing_logs (
    log_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id           UUID NOT NULL REFERENCES saving_lifecycle_schema.lifecycle_jobs(job_id),
    old_contract_no  VARCHAR(50) NOT NULL,
    new_contract_no  VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at     TIMESTAMP WITH TIME ZONE,
    error_message    TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_renewal_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE TABLE saving_lifecycle_schema.lifecycle_events (
    event_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_no  VARCHAR(50) NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    event_data   JSONB,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_lifecycle_event_type CHECK (
        event_type IN (
            'SAVING_CREATED', 'SAVING_MATURED', 'SAVING_RENEWED',
            'SAVING_CLOSED', 'INTEREST_PAID', 'TRANSACTION_FAILED'
        )
    )
);

-- Indexes for saving_lifecycle_schema
CREATE INDEX idx_lifecycle_jobs_status ON saving_lifecycle_schema.lifecycle_jobs(status);
CREATE INDEX idx_lifecycle_jobs_scan_date ON saving_lifecycle_schema.lifecycle_jobs(scan_date);
CREATE INDEX idx_maturity_logs_job ON saving_lifecycle_schema.maturity_processing_logs(job_id);
CREATE INDEX idx_maturity_logs_contract ON saving_lifecycle_schema.maturity_processing_logs(contract_no);
CREATE INDEX idx_renewal_logs_job ON saving_lifecycle_schema.renewal_processing_logs(job_id);
CREATE INDEX idx_lifecycle_events_contract ON saving_lifecycle_schema.lifecycle_events(contract_no);
CREATE INDEX idx_lifecycle_events_published ON saving_lifecycle_schema.lifecycle_events(is_published);

-- ==============================================================
-- NOTIFICATION_SCHEMA
-- ==============================================================

CREATE TABLE notification_schema.notification_templates (
    template_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code  VARCHAR(100) NOT NULL,
    channel        VARCHAR(30) NOT NULL,
    subject        VARCHAR(200),
    body_template  TEXT NOT NULL,
    language       VARCHAR(10) NOT NULL DEFAULT 'vi',
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_templates_code_channel UNIQUE (template_code, channel),
    CONSTRAINT chk_notif_channel CHECK (channel IN ('SMS', 'EMAIL', 'PUSH', 'IN_APP'))
);

CREATE TABLE notification_schema.notification_logs (
    log_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif             VARCHAR(20),
    template_code   VARCHAR(100),
    channel         VARCHAR(30),
    recipient       VARCHAR(200),
    content_summary TEXT,
    event_type      VARCHAR(50),
    correlation_id  VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP WITH TIME ZONE,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notif_log_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED'))
);

-- Indexes for notification_schema
CREATE INDEX idx_notification_templates_code ON notification_schema.notification_templates(template_code);
CREATE INDEX idx_notification_logs_cif ON notification_schema.notification_logs(cif);
CREATE INDEX idx_notification_logs_status ON notification_schema.notification_logs(status);
CREATE INDEX idx_notification_logs_event_type ON notification_schema.notification_logs(event_type);
CREATE INDEX idx_notification_logs_correlation ON notification_schema.notification_logs(correlation_id);

-- Notification service runtime table (NestJS TypeORM, synchronize=false in production)
CREATE TABLE notification_schema.notifications (
    notification_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif              VARCHAR(20)  NOT NULL,
    event_type       VARCHAR(50)  NOT NULL,
    contract_no      VARCHAR(50),
    channel          VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    recipient        VARCHAR(200),
    template_code    VARCHAR(50),
    content_summary  VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    correlation_id   VARCHAR(100),
    sent_at          TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_cif       ON notification_schema.notifications(cif);
CREATE INDEX idx_notifications_is_read   ON notification_schema.notifications(is_read);
CREATE INDEX idx_notifications_event_type ON notification_schema.notifications(event_type);
CREATE INDEX idx_notifications_created_at ON notification_schema.notifications(created_at DESC);

GRANT ALL PRIVILEGES ON notification_schema.notifications TO postgres;

-- ==============================================================
-- GRANT PERMISSIONS (for the main postgres user)
-- ==============================================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA customer_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA account_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA saving_product_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA saving_contract_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA transaction_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA interest_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA saving_lifecycle_schema TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notification_schema TO postgres;

GRANT USAGE ON SCHEMA auth_schema TO postgres;
GRANT USAGE ON SCHEMA customer_schema TO postgres;
GRANT USAGE ON SCHEMA account_schema TO postgres;
GRANT USAGE ON SCHEMA saving_product_schema TO postgres;
GRANT USAGE ON SCHEMA saving_contract_schema TO postgres;
GRANT USAGE ON SCHEMA transaction_schema TO postgres;
GRANT USAGE ON SCHEMA interest_schema TO postgres;
GRANT USAGE ON SCHEMA saving_lifecycle_schema TO postgres;
GRANT USAGE ON SCHEMA notification_schema TO postgres;
