-- =============================================================================
-- FinPilot V1 Initial Schema Definition
-- Compatible with PostgreSQL 16 & H2 (PostgreSQL Mode)
-- =============================================================================

-- 1. Users and RBAC
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 2. Ledger Accounts
CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(50) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    current_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Transactions
CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    external_reference VARCHAR(100),
    source_system VARCHAR(100) NOT NULL,
    source_account_number VARCHAR(50),
    destination_account_number VARCHAR(50),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    transaction_type VARCHAR(50) NOT NULL, -- PAYMENT, TRANSFER, REFUND, ADJUSTMENT, FEE
    status VARCHAR(50) NOT NULL, -- RECEIVED, VALIDATED, PENDING, POSTED, FAILED, REVERSED, REQUIRES_REVIEW
    idempotency_key VARCHAR(150) UNIQUE,
    description TEXT,
    metadata TEXT,
    transaction_timestamp TIMESTAMP NOT NULL,
    received_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_source ON transactions(source_system);
CREATE INDEX idx_transactions_ext_ref ON transactions(external_reference);

CREATE TABLE transaction_events (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(100) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_events_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

-- 4. Double-Entry Journal Entries
CREATE TABLE journal_entries (
    id VARCHAR(36) PRIMARY KEY,
    reference_id VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    posting_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, POSTED, REVERSED
    posted_timestamp TIMESTAMP,
    reversed_timestamp TIMESTAMP,
    reversal_of_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_entries_ref ON journal_entries(reference_id);
CREATE INDEX idx_journal_entries_status ON journal_entries(posting_status);

CREATE TABLE journal_entry_lines (
    id VARCHAR(36) PRIMARY KEY,
    journal_entry_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    debit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    credit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    description VARCHAR(255),
    line_number INT NOT NULL,
    CONSTRAINT fk_jel_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_jel_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_jel_journal_id ON journal_entry_lines(journal_entry_id);
CREATE INDEX idx_jel_account_id ON journal_entry_lines(account_id);

CREATE TABLE account_balance_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    snapshot_date TIMESTAMP NOT NULL,
    debit_total NUMERIC(19, 4) NOT NULL,
    credit_total NUMERIC(19, 4) NOT NULL,
    net_balance NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshot_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- 5. Reconciliation Engine
CREATE TABLE reconciliation_runs (
    id VARCHAR(36) PRIMARY KEY,
    run_number VARCHAR(50) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,
    matching_rules_version VARCHAR(20) NOT NULL DEFAULT 'v1.0',
    status VARCHAR(50) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    total_internal_records INT NOT NULL DEFAULT 0,
    total_external_records INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,
    execution_duration_ms BIGINT DEFAULT 0,
    error_details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE external_statement_records (
    id VARCHAR(36) PRIMARY KEY,
    reconciliation_run_id VARCHAR(36) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    record_date TIMESTAMP NOT NULL,
    raw_status VARCHAR(50),
    raw_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_esr_recon_run FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs(id) ON DELETE CASCADE
);

CREATE TABLE reconciliation_matches (
    id VARCHAR(36) PRIMARY KEY,
    reconciliation_run_id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(36),
    external_record_id VARCHAR(36),
    match_type VARCHAR(50) NOT NULL, -- LEVEL_1_EXACT, LEVEL_2_AMOUNT_MISMATCH, LEVEL_3_DATE_TOLERANCE, LEVEL_4_RULE_MATCH
    match_status VARCHAR(50) NOT NULL, -- MATCHED, AMOUNT_MISMATCH, MISSING_INTERNAL, MISSING_EXTERNAL, DUPLICATE, DATE_MISMATCH, NEEDS_REVIEW
    amount_difference NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    match_score INT NOT NULL DEFAULT 100,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_recon_run FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,
    CONSTRAINT fk_match_external FOREIGN KEY (external_record_id) REFERENCES external_statement_records(id) ON DELETE SET NULL
);

-- 6. Exception Management
CREATE TABLE exceptions (
    id VARCHAR(36) PRIMARY KEY,
    exception_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_id VARCHAR(36),
    reconciliation_run_id VARCHAR(36),
    category VARCHAR(100) NOT NULL, -- AMOUNT_DISCREPANCY, UNMATCHED_SETTLEMENT, DUPLICATE_ENTRY, DATE_DRIFT, POLICY_VIOLATION
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    description TEXT NOT NULL,
    financial_impact NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, INVESTIGATING, PENDING_APPROVAL, RESOLVED, REJECTED, CLOSED
    assigned_to_user_id VARCHAR(36),
    root_cause TEXT,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exc_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,
    CONSTRAINT fk_exc_recon FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs(id) ON DELETE SET NULL,
    CONSTRAINT fk_exc_assigned FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE exception_comments (
    id VARCHAR(36) PRIMARY KEY,
    exception_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_exc FOREIGN KEY (exception_id) REFERENCES exceptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- 7. AI Investigations
CREATE TABLE investigations (
    id VARCHAR(36) PRIMARY KEY,
    exception_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED
    initiated_by VARCHAR(36) NOT NULL,
    summary TEXT,
    root_cause_analysis TEXT,
    recommended_action TEXT,
    confidence_score NUMERIC(5, 2),
    evidence_payload TEXT,
    policy_validation_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_inv_exc FOREIGN KEY (exception_id) REFERENCES exceptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_user FOREIGN KEY (initiated_by) REFERENCES users(id)
);

CREATE TABLE agent_execution_events (
    id VARCHAR(36) PRIMARY KEY,
    investigation_id VARCHAR(36) NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    tool_called VARCHAR(100),
    tool_input TEXT,
    tool_output TEXT,
    execution_duration_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aee_inv FOREIGN KEY (investigation_id) REFERENCES investigations(id) ON DELETE CASCADE
);

-- 8. Resolution Proposals & Human Approval Center
CREATE TABLE resolution_proposals (
    id VARCHAR(36) PRIMARY KEY,
    investigation_id VARCHAR(36),
    exception_id VARCHAR(36) NOT NULL,
    proposed_action VARCHAR(100) NOT NULL, -- POST_LEDGER_ADJUSTMENT, REVERSE_TRANSACTION, WRITE_OFF, MANUAL_MATCH
    adjustment_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    debit_account_id VARCHAR(36),
    credit_account_id VARCHAR(36),
    rationale TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED', -- DRAFT, SUBMITTED, APPROVED, REJECTED, EXECUTED
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prop_inv FOREIGN KEY (investigation_id) REFERENCES investigations(id) ON DELETE SET NULL,
    CONSTRAINT fk_prop_exc FOREIGN KEY (exception_id) REFERENCES exceptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_prop_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_prop_debit_acc FOREIGN KEY (debit_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_prop_credit_acc FOREIGN KEY (credit_account_id) REFERENCES accounts(id)
);

CREATE TABLE approval_requests (
    id VARCHAR(36) PRIMARY KEY,
    request_number VARCHAR(50) NOT NULL UNIQUE,
    resolution_proposal_id VARCHAR(36) NOT NULL,
    requested_by VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, EXPIRED
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_appr_prop FOREIGN KEY (resolution_proposal_id) REFERENCES resolution_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_appr_user FOREIGN KEY (requested_by) REFERENCES users(id)
);

CREATE TABLE approval_decisions (
    id VARCHAR(36) PRIMARY KEY,
    approval_request_id VARCHAR(36) NOT NULL,
    decided_by VARCHAR(36) NOT NULL,
    decision VARCHAR(50) NOT NULL, -- APPROVED, REJECTED
    comments TEXT,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dec_req FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_dec_user FOREIGN KEY (decided_by) REFERENCES users(id)
);

-- 9. Audit Logs & Outbox Events
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL, -- CREATE, UPDATE, POST, APPROVE, REJECT, REVERSE, INVESTIGATE
    performed_by VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100),
    ip_address VARCHAR(50),
    before_state TEXT,
    after_state TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
