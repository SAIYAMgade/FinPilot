-- =============================================================================
-- FinPilot V2 Seed Data
-- Demo Roles, Users, Chart of Accounts, and Initial Sample Data
-- =============================================================================

-- Seed Roles
INSERT INTO roles (id, name, description) VALUES
('role-001', 'ADMIN', 'Full platform administrator with configuration privileges'),
('role-002', 'FINANCE_ANALYST', 'Finance analyst: ingest statements, trigger recon, investigate exceptions, propose adjustments'),
('role-003', 'FINANCE_APPROVER', 'Finance approver: review and approve or reject adjustment requests'),
('role-004', 'AUDITOR', 'Auditor: read-only access to transactions, ledgers, reconciliations, and audit trails')
ON CONFLICT (id) DO NOTHING;

-- Seed Standard Chart of Accounts
INSERT INTO accounts (id, account_number, account_name, account_type, currency, status, current_balance) VALUES
('acc-1010', '1010', 'Cash & Settlement Clearing', 'ASSET', 'INR', 'ACTIVE', 5000000.0000),
('acc-1020', '1020', 'Accounts Receivable - Settlements', 'ASSET', 'INR', 'ACTIVE', 2500000.0000),
('acc-1030', '1030', 'Reconciliation Suspense Account', 'ASSET', 'INR', 'ACTIVE', 0.0000),
('acc-2010', '2010', 'Accounts Payable - Merchant Payouts', 'LIABILITY', 'INR', 'ACTIVE', 3500000.0000),
('acc-3010', '3010', 'Retained Earnings', 'EQUITY', 'INR', 'ACTIVE', 4000000.0000),
('acc-4010', '4010', 'Processing Fee Revenue', 'REVENUE', 'INR', 'ACTIVE', 125000.0000),
('acc-5010', '5010', 'Interchange & Gateway Fee Expense', 'EXPENSE', 'INR', 'ACTIVE', 35000.0000),
('acc-5020', '5020', 'Reconciliation Discrepancy Write-Off', 'EXPENSE', 'INR', 'ACTIVE', 10000.0000)
ON CONFLICT (id) DO NOTHING;
