# Database Design — Saving Banking Microservices

## Nguyên tắc

- Mỗi service sở hữu 1 schema trong PostgreSQL `saving_banking`
- Không JOIN cross-schema ở runtime — chỉ gọi API hoặc event
- `cif`, `account_no`, `contract_no` là **logical reference** (không FOREIGN KEY vật lý cross-schema)
- Optimistic locking dùng cột `version` (BIGINT, tăng mỗi update)

## Schema Map

| Schema | Owned by Service | Tables |
|---|---|---|
| `auth_schema` | Auth Service | users, roles, permissions, user_roles, otp_requests, login_sessions |
| `customer_schema` | Customer Service | customers, customer_kyc, customer_contacts |
| `account_schema` | Account Service | accounts, account_balances, account_hold_logs |
| `saving_product_schema` | Saving Product Service | saving_products, saving_terms, interest_rate_configs, early_withdrawal_policies |
| `saving_contract_schema` | Saving Contract Service | saving_contracts, maturity_instructions, saving_contract_status_history |
| `transaction_schema` | Transaction Service | transactions, ledger_entries, core_banking_sync_logs, outbox_events |
| `interest_schema` | Interest Calc. Service | interest_calculations, interest_schedules, interest_payment_history |
| `saving_lifecycle_schema` | Saving Lifecycle Service | lifecycle_jobs, maturity_processing_logs, renewal_processing_logs, lifecycle_events |
| `notification_schema` | Notification Service | notification_templates, notification_logs |

## Key Enums

### ContractStatus
`PENDING` → `ACTIVE` → `MATURED` → `CLOSED` / `EARLY_CLOSED`

### TransactionStatus
`PENDING` → `SUCCESS` / `FAILED` / `REVERSED`

### InterestPaymentMethod
`END_OF_TERM` | `MONTHLY` | `QUARTERLY` | `UPFRONT`

### MaturityInstructionType
`TRANSFER_PRINCIPAL_AND_INTEREST` | `RENEW_PRINCIPAL` | `RENEW_PRINCIPAL_AND_INTEREST`
