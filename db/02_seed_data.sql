-- ==============================================================
-- SAVING BANKING SYSTEM - SEED DATA
-- ==============================================================
-- Default passwords:
--   admin     → Admin@123
--   teller01  → Teller@123
--   customer001, customer002 → Test@123
-- BCrypt hashes generated with cost factor 10
-- ==============================================================

-- ==============================================================
-- AUTH_SCHEMA SEED DATA
-- ==============================================================

-- Roles
INSERT INTO auth_schema.roles (role_id, role_code, role_name, description) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'ADMIN',    'System Administrator', 'Full system access'),
    ('a0000001-0000-0000-0000-000000000002', 'TELLER',   'Bank Teller',          'Counter operations, open/close saving'),
    ('a0000001-0000-0000-0000-000000000003', 'CUSTOMER', 'Customer',             'Self-service banking'),
    ('a0000001-0000-0000-0000-000000000004', 'MANAGER',  'Branch Manager',       'Approve transactions, reports'),
    ('a0000001-0000-0000-0000-000000000005', 'SYSTEM',   'System Service',       'Internal service calls');

-- Permissions
INSERT INTO auth_schema.permissions (permission_id, permission_code, resource, action) VALUES
    ('b0000001-0000-0000-0000-000000000001', 'SAVING_CREATE',     'saving-contract', 'CREATE'),
    ('b0000001-0000-0000-0000-000000000002', 'SAVING_READ',       'saving-contract', 'READ'),
    ('b0000001-0000-0000-0000-000000000003', 'SAVING_CLOSE',      'saving-contract', 'CLOSE'),
    ('b0000001-0000-0000-0000-000000000004', 'ACCOUNT_DEBIT',     'account',         'DEBIT'),
    ('b0000001-0000-0000-0000-000000000005', 'ACCOUNT_CREDIT',    'account',         'CREDIT'),
    ('b0000001-0000-0000-0000-000000000006', 'CUSTOMER_READ',     'customer',        'READ'),
    ('b0000001-0000-0000-0000-000000000007', 'PRODUCT_READ',      'saving-product',  'READ'),
    ('b0000001-0000-0000-0000-000000000008', 'TRANSACTION_READ',  'transaction',     'READ'),
    ('b0000001-0000-0000-0000-000000000009', 'REPORT_READ',       'report',          'READ'),
    ('b0000001-0000-0000-0000-000000000010', 'ADMIN_ALL',         '*',               'ALL');

-- Users
-- admin / Admin@123
INSERT INTO auth_schema.users (user_id, username, password_hash, cif, status) VALUES
    ('c0000001-0000-0000-0000-000000000001',
     'admin',
     '$2b$10$/4vqeZ5pdp/rop7upw.PresohD9AVpLsX4UYvQtSh8WibNjzU6Dlm',
     NULL,
     'ACTIVE');

-- teller01 / Teller@123
INSERT INTO auth_schema.users (user_id, username, password_hash, cif, status) VALUES
    ('c0000001-0000-0000-0000-000000000002',
     'teller01',
     '$2b$10$waUK8i3H4SpesR1zpZeg9OZSceZcMR9jx/mljeQh/lxI4O9jkClji',
     NULL,
     'ACTIVE');

-- customer001 / Test@123 → CIF0001
INSERT INTO auth_schema.users (user_id, username, password_hash, cif, status) VALUES
    ('c0000001-0000-0000-0000-000000000003',
     'customer001',
     '$2b$10$ATuIjTH5W37XhQazTQxn2O6qCDFxmA1lr35.nQLZVfmPWgAFIevJi',
     'CIF0001',
     'ACTIVE');

-- customer002 / Test@123 → CIF0002
INSERT INTO auth_schema.users (user_id, username, password_hash, cif, status) VALUES
    ('c0000001-0000-0000-0000-000000000004',
     'customer002',
     '$2b$10$ATuIjTH5W37XhQazTQxn2O6qCDFxmA1lr35.nQLZVfmPWgAFIevJi',
     'CIF0002',
     'ACTIVE');

-- User-Role assignments
INSERT INTO auth_schema.user_roles (user_id, role_id) VALUES
    ('c0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001'), -- admin → ADMIN
    ('c0000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000002'), -- teller01 → TELLER
    ('c0000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000003'), -- customer001 → CUSTOMER
    ('c0000001-0000-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000003'); -- customer002 → CUSTOMER

-- ==============================================================
-- CUSTOMER_SCHEMA SEED DATA
-- ==============================================================

INSERT INTO customer_schema.customers (cif, full_name, date_of_birth, gender, nationality, id_number, id_type, status) VALUES
    ('CIF0001', 'Nguyen Van An',   '1990-05-15', 'MALE',   'VN', '012345678901', 'NATIONAL_ID', 'ACTIVE'),
    ('CIF0002', 'Tran Thi Bich',   '1985-08-20', 'FEMALE', 'VN', '098765432101', 'NATIONAL_ID', 'ACTIVE'),
    ('CIF0003', 'Le Hoang Minh',   '1978-03-10', 'MALE',   'VN', '055512349876', 'NATIONAL_ID', 'ACTIVE'),
    ('CIF0004', 'Pham Ngoc Lan',   '1995-11-28', 'FEMALE', 'VN', '034512398765', 'NATIONAL_ID', 'ACTIVE');

INSERT INTO customer_schema.customer_kyc (kyc_id, cif, kyc_status, verified_at, verified_by, doc_type) VALUES
    ('d0000001-0000-0000-0000-000000000001', 'CIF0001', 'VERIFIED', NOW() - INTERVAL '30 days', 'STAFF_001', 'NATIONAL_ID'),
    ('d0000001-0000-0000-0000-000000000002', 'CIF0002', 'VERIFIED', NOW() - INTERVAL '20 days', 'STAFF_001', 'NATIONAL_ID'),
    ('d0000001-0000-0000-0000-000000000003', 'CIF0003', 'VERIFIED', NOW() - INTERVAL '10 days', 'STAFF_002', 'NATIONAL_ID'),
    ('d0000001-0000-0000-0000-000000000004', 'CIF0004', 'PENDING',  NULL,                       NULL,        'NATIONAL_ID');

INSERT INTO customer_schema.customer_contacts (contact_id, cif, phone_number, email, address, district, city, is_primary) VALUES
    ('e0000001-0000-0000-0000-000000000001', 'CIF0001', '0901234567', 'nguyen.van.an@email.com',  '123 Nguyen Hue St',    'District 1',    'Ho Chi Minh', TRUE),
    ('e0000001-0000-0000-0000-000000000002', 'CIF0002', '0912345678', 'tran.thi.bich@email.com',  '456 Le Loi St',       'Hoan Kiem',     'Ha Noi',      TRUE),
    ('e0000001-0000-0000-0000-000000000003', 'CIF0003', '0923456789', 'le.hoang.minh@email.com',  '789 Tran Phu St',     'Hai Chau',      'Da Nang',     TRUE),
    ('e0000001-0000-0000-0000-000000000004', 'CIF0004', '0934567890', 'pham.ngoc.lan@email.com',  '321 Bach Dang St',    'Thanh Khe',     'Da Nang',     TRUE);

-- ==============================================================
-- ACCOUNT_SCHEMA SEED DATA
-- ==============================================================

INSERT INTO account_schema.accounts (account_no, cif, account_type, currency, status, open_date, branch_code) VALUES
    ('ACC001001', 'CIF0001', 'PAYMENT', 'VND', 'ACTIVE', '2023-01-15', 'HCM001'),
    ('ACC001002', 'CIF0001', 'PAYMENT', 'VND', 'ACTIVE', '2023-06-01', 'HCM001'),
    ('ACC002001', 'CIF0002', 'PAYMENT', 'VND', 'ACTIVE', '2022-05-20', 'HAN001'),
    ('ACC003001', 'CIF0003', 'PAYMENT', 'VND', 'ACTIVE', '2021-11-10', 'DAN001'),
    ('ACC004001', 'CIF0004', 'PAYMENT', 'VND', 'ACTIVE', '2024-01-05', 'DAN001');

INSERT INTO account_schema.account_balances (balance_id, account_no, available_balance, ledger_balance, hold_amount, currency, version) VALUES
    ('f0000001-0000-0000-0000-000000000001', 'ACC001001', 150000000.00, 150000000.00, 0.00, 'VND', 0),
    ('f0000001-0000-0000-0000-000000000002', 'ACC001002',  50000000.00,  50000000.00, 0.00, 'VND', 0),
    ('f0000001-0000-0000-0000-000000000003', 'ACC002001', 200000000.00, 200000000.00, 0.00, 'VND', 0),
    ('f0000001-0000-0000-0000-000000000004', 'ACC003001',  80000000.00,  80000000.00, 0.00, 'VND', 0),
    ('f0000001-0000-0000-0000-000000000005', 'ACC004001',  30000000.00,  30000000.00, 0.00, 'VND', 0);

-- ==============================================================
-- SAVING_PRODUCT_SCHEMA SEED DATA
-- ==============================================================

-- Products
INSERT INTO saving_product_schema.saving_products
    (product_code, product_name, currency, min_amount, max_amount, interest_payment_method, is_active, description)
VALUES
    ('TERM_SAVING_EOT', 'Tiet kiem co ky han - Linh lai cuoi ky',
     'VND', 1000000, 10000000000, 'END_OF_TERM', TRUE,
     'Gui tiet kiem co ky han, linh lai va goc khi den han'),
    ('TERM_SAVING_MONTHLY', 'Tiet kiem co ky han - Tra lai hang thang',
     'VND', 5000000, 10000000000, 'MONTHLY', TRUE,
     'Gui tiet kiem co ky han, tra lai hang thang vao TK thanh toan'),
    ('TERM_SAVING_QUARTERLY', 'Tiet kiem co ky han - Tra lai hang quy',
     'VND', 10000000, 10000000000, 'QUARTERLY', TRUE,
     'Gui tiet kiem co ky han, tra lai hang quy');

-- Terms
INSERT INTO saving_product_schema.saving_terms
    (term_id, product_code, term_months, term_days, term_label, is_active)
VALUES
    -- END_OF_TERM product terms
    ('TERM_EOT_1M',  'TERM_SAVING_EOT',      1,  30,  '1 thang',   TRUE),
    ('TERM_EOT_3M',  'TERM_SAVING_EOT',      3,  91,  '3 thang',   TRUE),
    ('TERM_EOT_6M',  'TERM_SAVING_EOT',      6,  182, '6 thang',   TRUE),
    ('TERM_EOT_9M',  'TERM_SAVING_EOT',      9,  274, '9 thang',   TRUE),
    ('TERM_EOT_12M', 'TERM_SAVING_EOT',     12,  365, '12 thang',  TRUE),
    ('TERM_EOT_18M', 'TERM_SAVING_EOT',     18,  548, '18 thang',  TRUE),
    ('TERM_EOT_24M', 'TERM_SAVING_EOT',     24,  730, '24 thang',  TRUE),
    -- MONTHLY product terms
    ('TERM_MTH_6M',  'TERM_SAVING_MONTHLY',  6,  182, '6 thang',   TRUE),
    ('TERM_MTH_12M', 'TERM_SAVING_MONTHLY', 12,  365, '12 thang',  TRUE),
    ('TERM_MTH_24M', 'TERM_SAVING_MONTHLY', 24,  730, '24 thang',  TRUE),
    -- QUARTERLY product terms
    ('TERM_QTR_12M', 'TERM_SAVING_QUARTERLY',12, 365, '12 thang',  TRUE),
    ('TERM_QTR_24M', 'TERM_SAVING_QUARTERLY',24, 730, '24 thang',  TRUE);

-- Interest rate configs (effective from Jan 1, 2025)
INSERT INTO saving_product_schema.interest_rate_configs
    (product_code, term_id, annual_rate, effective_from, effective_to, is_active)
VALUES
    -- TERM_SAVING_EOT rates
    ('TERM_SAVING_EOT', 'TERM_EOT_1M',  3.5,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_3M',  4.2,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_6M',  5.5,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_9M',  5.8,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_12M', 6.2,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_18M', 6.5,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_EOT', 'TERM_EOT_24M', 6.8,  '2025-01-01', NULL, TRUE),
    -- TERM_SAVING_MONTHLY rates
    ('TERM_SAVING_MONTHLY', 'TERM_MTH_6M',  5.3,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_MONTHLY', 'TERM_MTH_12M', 6.0,  '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_MONTHLY', 'TERM_MTH_24M', 6.5,  '2025-01-01', NULL, TRUE),
    -- TERM_SAVING_QUARTERLY rates
    ('TERM_SAVING_QUARTERLY', 'TERM_QTR_12M', 6.1, '2025-01-01', NULL, TRUE),
    ('TERM_SAVING_QUARTERLY', 'TERM_QTR_24M', 6.6, '2025-01-01', NULL, TRUE);

-- Early withdrawal policies (penalty: demand rate = 0.5%/year)
INSERT INTO saving_product_schema.early_withdrawal_policies
    (product_code, min_days_held, penalty_rate, use_demand_rate, demand_rate)
VALUES
    ('TERM_SAVING_EOT',       0, 0.0, TRUE, 0.5),
    ('TERM_SAVING_MONTHLY',   0, 0.0, TRUE, 0.5),
    ('TERM_SAVING_QUARTERLY', 0, 0.0, TRUE, 0.5);

-- ==============================================================
-- SAVING_CONTRACT_SCHEMA SEED DATA
-- ==============================================================

-- Active saving contract for CIF0001
INSERT INTO saving_contract_schema.saving_contracts
    (contract_no, cif, product_code, term_id, principal_amount, interest_rate,
     currency, open_date, maturity_date, status, interest_payment_method,
     source_account_no, branch_code, opened_by, version)
VALUES
    ('SC-2025-000001', 'CIF0001', 'TERM_SAVING_EOT', 'TERM_EOT_6M',
     50000000.00, 5.5, 'VND',
     CURRENT_DATE - INTERVAL '90 days',
     CURRENT_DATE + INTERVAL '92 days',
     'ACTIVE', 'END_OF_TERM', 'ACC001001', 'HCM001', 'STAFF_001', 1),

    ('SC-2025-000002', 'CIF0002', 'TERM_SAVING_EOT', 'TERM_EOT_12M',
     100000000.00, 6.2, 'VND',
     CURRENT_DATE - INTERVAL '30 days',
     CURRENT_DATE + INTERVAL '335 days',
     'ACTIVE', 'END_OF_TERM', 'ACC002001', 'HAN001', 'STAFF_002', 1),

    ('SC-2025-000003', 'CIF0001', 'TERM_SAVING_MONTHLY', 'TERM_MTH_12M',
     30000000.00, 6.0, 'VND',
     CURRENT_DATE - INTERVAL '60 days',
     CURRENT_DATE + INTERVAL '305 days',
     'ACTIVE', 'MONTHLY', 'ACC001002', 'HCM001', 'STAFF_001', 1),

    -- Near maturity contract (for lifecycle testing)
    ('SC-2025-000004', 'CIF0003', 'TERM_SAVING_EOT', 'TERM_EOT_3M',
     20000000.00, 4.2, 'VND',
     CURRENT_DATE - INTERVAL '89 days',
     CURRENT_DATE + INTERVAL '2 days',
     'ACTIVE', 'END_OF_TERM', 'ACC003001', 'DAN001', 'STAFF_001', 1),

    -- Already closed contract
    ('SC-2024-000001', 'CIF0001', 'TERM_SAVING_EOT', 'TERM_EOT_3M',
     10000000.00, 4.2, 'VND',
     CURRENT_DATE - INTERVAL '180 days',
     CURRENT_DATE - INTERVAL '89 days',
     'CLOSED', 'END_OF_TERM', 'ACC001001', 'HCM001', 'STAFF_001', 2);

-- Maturity instructions
INSERT INTO saving_contract_schema.maturity_instructions
    (contract_no, instruction_type, new_term_id, receiving_account_no)
VALUES
    ('SC-2025-000001', 'RENEW_PRINCIPAL',                'TERM_EOT_6M',  'ACC001001'),
    ('SC-2025-000002', 'RENEW_PRINCIPAL_AND_INTEREST',   'TERM_EOT_12M', 'ACC002001'),
    ('SC-2025-000003', 'TRANSFER_PRINCIPAL_AND_INTEREST', NULL,           'ACC001002'),
    ('SC-2025-000004', 'RENEW_PRINCIPAL',                'TERM_EOT_3M',  'ACC003001');

-- Status history
INSERT INTO saving_contract_schema.saving_contract_status_history
    (contract_no, from_status, to_status, changed_by, reason)
VALUES
    ('SC-2025-000001', 'PENDING', 'ACTIVE', 'STAFF_001', 'Transaction confirmed'),
    ('SC-2025-000002', 'PENDING', 'ACTIVE', 'STAFF_002', 'Transaction confirmed'),
    ('SC-2025-000003', 'PENDING', 'ACTIVE', 'STAFF_001', 'Transaction confirmed'),
    ('SC-2025-000004', 'PENDING', 'ACTIVE', 'STAFF_001', 'Transaction confirmed'),
    ('SC-2024-000001', 'PENDING', 'ACTIVE', 'STAFF_001', 'Transaction confirmed'),
    ('SC-2024-000001', 'ACTIVE',  'CLOSED', 'SYSTEM',    'Maturity processed - TRANSFER_PRINCIPAL_AND_INTEREST');

-- ==============================================================
-- TRANSACTION_SCHEMA SEED DATA
-- ==============================================================

INSERT INTO transaction_schema.transactions
    (transaction_id, transaction_type, contract_no, cif, amount, currency,
     account_no, status, transaction_ref, correlation_id, cbs_reference, description)
VALUES
    ('c0000001-0000-0000-0000-000000000001',
     'OPEN_SAVING', 'SC-2025-000001', 'CIF0001', 50000000.00, 'VND',
     'ACC001001', 'SUCCESS',
     'OPEN-SC-2025-000001-20250115',
     'CORR-00000001', 'CBS-TX-2025-000001',
     'Mo so tiet kiem SC-2025-000001'),

    ('c0000001-0000-0000-0000-000000000002',
     'OPEN_SAVING', 'SC-2025-000002', 'CIF0002', 100000000.00, 'VND',
     'ACC002001', 'SUCCESS',
     'OPEN-SC-2025-000002-20250215',
     'CORR-00000002', 'CBS-TX-2025-000002',
     'Mo so tiet kiem SC-2025-000002'),

    ('c0000001-0000-0000-0000-000000000003',
     'OPEN_SAVING', 'SC-2025-000003', 'CIF0001', 30000000.00, 'VND',
     'ACC001002', 'SUCCESS',
     'OPEN-SC-2025-000003-20250316',
     'CORR-00000003', 'CBS-TX-2025-000003',
     'Mo so tiet kiem SC-2025-000003'),

    ('c0000001-0000-0000-0000-000000000004',
     'OPEN_SAVING', 'SC-2025-000004', 'CIF0003', 20000000.00, 'VND',
     'ACC003001', 'SUCCESS',
     'OPEN-SC-2025-000004-20250318',
     'CORR-00000004', 'CBS-TX-2025-000004',
     'Mo so tiet kiem SC-2025-000004'),

    -- Closed contract transactions
    ('c0000001-0000-0000-0000-000000000005',
     'OPEN_SAVING', 'SC-2024-000001', 'CIF0001', 10000000.00, 'VND',
     'ACC001001', 'SUCCESS',
     'OPEN-SC-2024-000001-20240101',
     'CORR-00000005', 'CBS-TX-2024-000001',
     'Mo so tiet kiem SC-2024-000001'),

    ('c0000001-0000-0000-0000-000000000006',
     'CLOSE_SAVING', 'SC-2024-000001', 'CIF0001', 10103836.00, 'VND',
     NULL, 'SUCCESS',
     'CLOSE-SC-2024-000001-20240401',
     'CORR-00000006', 'CBS-TX-2024-000002',
     'Tat toan so tiet kiem SC-2024-000001 (goc + lai)');

-- Ledger entries (tx_id FK references transaction_schema.transactions.transaction_id)
INSERT INTO transaction_schema.ledger_entries
    (tx_id, debit_account, credit_account, amount, currency, entry_date, description)
VALUES
    ('c0000001-0000-0000-0000-000000000001',
     'ACC001001', 'SAVING-POOL-VND', 50000000.00, 'VND',
     CURRENT_DATE - INTERVAL '90 days', 'Debit - Mo so TK SC-2025-000001'),

    ('c0000001-0000-0000-0000-000000000002',
     'ACC002001', 'SAVING-POOL-VND', 100000000.00, 'VND',
     CURRENT_DATE - INTERVAL '30 days', 'Debit - Mo so TK SC-2025-000002'),

    ('c0000001-0000-0000-0000-000000000003',
     'ACC001002', 'SAVING-POOL-VND', 30000000.00, 'VND',
     CURRENT_DATE - INTERVAL '60 days', 'Debit - Mo so TK SC-2025-000003'),

    ('c0000001-0000-0000-0000-000000000005',
     'ACC001001', 'SAVING-POOL-VND', 10000000.00, 'VND',
     CURRENT_DATE - INTERVAL '180 days', 'Debit - Mo so TK SC-2024-000001'),

    ('c0000001-0000-0000-0000-000000000006',
     'SAVING-POOL-VND', 'ACC001001', 10103836.00, 'VND',
     CURRENT_DATE - INTERVAL '89 days', 'Credit - Tat toan SC-2024-000001');

-- ==============================================================
-- INTEREST_SCHEMA SEED DATA
-- ==============================================================

-- Pre-generated interest calculations for active contracts
INSERT INTO interest_schema.interest_calculations
    (contract_no, calc_type, principal, annual_rate, from_date, to_date, days, interest_amount)
VALUES
    ('SC-2025-000001', 'EXPECTED', 50000000.00, 5.5,
     CURRENT_DATE - INTERVAL '90 days',
     CURRENT_DATE + INTERVAL '92 days',
     182,
     ROUND(50000000.00 * 5.5 / 100 * 182 / 365, 2)),

    ('SC-2025-000002', 'EXPECTED', 100000000.00, 6.2,
     CURRENT_DATE - INTERVAL '30 days',
     CURRENT_DATE + INTERVAL '335 days',
     365,
     ROUND(100000000.00 * 6.2 / 100 * 365 / 365, 2)),

    ('SC-2024-000001', 'MATURITY', 10000000.00, 4.2,
     CURRENT_DATE - INTERVAL '180 days',
     CURRENT_DATE - INTERVAL '89 days',
     91,
     ROUND(10000000.00 * 4.2 / 100 * 91 / 365, 2));

-- Interest schedules for monthly payment contract
INSERT INTO interest_schema.interest_schedules
    (contract_no, payment_date, expected_interest, actual_interest, status, paid_at)
VALUES
    -- SC-2025-000003 monthly schedule (6.0%/year, principal 30M)
    ('SC-2025-000003',
     CURRENT_DATE - INTERVAL '60 days' + INTERVAL '30 days',
     ROUND(30000000.00 * 6.0 / 100 * 30 / 365, 2),
     ROUND(30000000.00 * 6.0 / 100 * 30 / 365, 2),
     'PAID',
     CURRENT_DATE - INTERVAL '30 days'),

    ('SC-2025-000003',
     CURRENT_DATE - INTERVAL '60 days' + INTERVAL '60 days',
     ROUND(30000000.00 * 6.0 / 100 * 30 / 365, 2),
     NULL, 'PENDING', NULL),

    ('SC-2025-000003',
     CURRENT_DATE - INTERVAL '60 days' + INTERVAL '90 days',
     ROUND(30000000.00 * 6.0 / 100 * 30 / 365, 2),
     NULL, 'PENDING', NULL);

-- ==============================================================
-- NOTIFICATION_SCHEMA SEED DATA
-- ==============================================================

INSERT INTO notification_schema.notification_templates
    (template_code, channel, subject, body_template, language, is_active)
VALUES
    -- SMS templates
    ('SAVING_CREATED_SMS', 'SMS', NULL,
     'So tiet kiem {{contractNo}} da mo thanh cong. So tien: {{amount}} VND. Ngay dao han: {{maturityDate}}. Cam on Quy khach!',
     'vi', TRUE),

    ('SAVING_CLOSED_SMS', 'SMS', NULL,
     'So tiet kiem {{contractNo}} da tat toan. Tong nhan: {{totalAmount}} VND (Goc: {{principal}} + Lai: {{interest}}). Cam on Quy khach!',
     'vi', TRUE),

    ('SAVING_MATURED_SMS', 'SMS', NULL,
     'So tiet kiem {{contractNo}} da den han ngay {{maturityDate}}. He thong da xu ly theo chi thi: {{instruction}}. Cam on Quy khach!',
     'vi', TRUE),

    ('SAVING_RENEWED_SMS', 'SMS', NULL,
     'So tiet kiem {{oldContractNo}} da tai tuc. So moi: {{newContractNo}}. Goc moi: {{principal}} VND. Cam on Quy khach!',
     'vi', TRUE),

    ('INTEREST_PAID_SMS', 'SMS', NULL,
     'Lai tiet kiem ky {{paymentDate}}: {{amount}} VND tu so {{contractNo}} da duoc chuyen vao TK {{accountNo}}.',
     'vi', TRUE),

    -- EMAIL templates
    ('SAVING_CREATED_EMAIL', 'EMAIL', 'Xac nhan mo so tiet kiem - {{contractNo}}',
     '<h2>Chao {{fullName}}</h2><p>So tiet kiem <strong>{{contractNo}}</strong> da mo thanh cong.</p><ul><li>So tien gui: {{amount}} VND</li><li>Ky han: {{term}}</li><li>Lai suat: {{rate}}%/nam</li><li>Ngay dao han: {{maturityDate}}</li></ul><p>Cam on Quy khach da tin tuong!</p>',
     'vi', TRUE),

    ('SAVING_CLOSED_EMAIL', 'EMAIL', 'Xac nhan tat toan so tiet kiem - {{contractNo}}',
     '<h2>Chao {{fullName}}</h2><p>So tiet kiem <strong>{{contractNo}}</strong> da tat toan.</p><ul><li>So tien goc: {{principal}} VND</li><li>Lai nhan duoc: {{interest}} VND</li><li>Tong nhan: {{totalAmount}} VND</li></ul><p>Cam on Quy khach!</p>',
     'vi', TRUE),

    -- PUSH templates
    ('SAVING_CREATED_PUSH', 'PUSH', 'Mo so tiet kiem thanh cong',
     'So TK {{contractNo}} da mo. So tien {{amount}} VND ky han {{term}}.',
     'vi', TRUE),

    ('SAVING_MATURED_PUSH', 'PUSH', 'So tiet kiem den han',
     'So TK {{contractNo}} den han hom nay. He thong da xu ly tu dong.',
     'vi', TRUE);

-- Sample notification logs
INSERT INTO notification_schema.notification_logs
    (cif, template_code, channel, recipient, content_summary,
     event_type, correlation_id, status, sent_at)
VALUES
    ('CIF0001', 'SAVING_CREATED_SMS', 'SMS', '090****567',
     'Mo so SC-2025-000001 thanh cong - 50.000.000 VND',
     'SAVING_CREATED', 'CORR-00000001', 'SENT', NOW() - INTERVAL '90 days'),

    ('CIF0002', 'SAVING_CREATED_SMS', 'SMS', '091****678',
     'Mo so SC-2025-000002 thanh cong - 100.000.000 VND',
     'SAVING_CREATED', 'CORR-00000002', 'SENT', NOW() - INTERVAL '30 days'),

    ('CIF0001', 'SAVING_CREATED_EMAIL', 'EMAIL', 'nguy****@email.com',
     'Xac nhan mo so SC-2025-000001',
     'SAVING_CREATED', 'CORR-00000001', 'SENT', NOW() - INTERVAL '90 days'),

    ('CIF0001', 'SAVING_CLOSED_SMS', 'SMS', '090****567',
     'Tat toan SC-2024-000001 - Nhan: 10.103.836 VND',
     'SAVING_CLOSED', 'CORR-00000006', 'SENT', NOW() - INTERVAL '89 days');
