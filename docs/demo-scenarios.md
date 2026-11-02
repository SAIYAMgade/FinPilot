# FinPilot: End-to-End Demo & Evaluation Walkthrough

Follow this 10-step guided demo scenario to test every core capability of FinPilot.

---

## Pre-Seeded Operator Credentials

| Persona Role | Username | Password | Intended Capabilities |
|---|---|---|---|
| **Finance Analyst** | `analyst` | `Password@123` | Ingest payments, execute reconciliation, trigger AI investigation, submit adjustment proposals |
| **Finance Approver** | `approver` | `Password@123` | Review and authorize/reject financial adjustment requests (Maker-Checker) |
| **Administrator** | `admin` | `Password@123` | Full chart of accounts configuration, user management, and security controls |
| **Compliance Auditor** | `auditor` | `Password@123` | Read-only inspection of transactions, trial balances, and immutable audit logs |

---

## 10-Step Walkthrough Scenario

### Step 1: Login as Finance Analyst
1. Navigate to `http://localhost:3000/login` (or click the quick switcher button **"Finance Analyst"**).
2. Login with `analyst` / `Password@123`.
3. Observe the Operations Command Center dashboard with live KPI cards and daily volume charts.

### Step 2: Ingest a Transaction with Idempotency Protection
1. Navigate to **Transactions Feed** (`/transactions`).
2. Click **"Ingest Transaction"**.
3. Enter Reference: `TXN-DEMO-01`, Amount: `50000.00`, Source: `PAYGATE-IN`, Idempotency Key: `idemp-demo-01`.
4. Click **"Ingest Transaction"**.
5. Re-submit the identical form. Note that FinPilot detects the idempotent replay and returns the existing transaction with zero duplicated records!

### Step 3: Upload External Bank Statement & Start Reconciliation
1. Navigate to **New Reconciliation** (`/reconciliations/new`).
2. Click **"Download Sample CSV"** to get `sample_bank_settlement.csv`.
3. Upload the statement CSV file.
4. Set Date Tolerance to `2 Days`.
5. Click **"Execute 4-Tier Reconciliation Match"**.

### Step 4: Inspect 4-Tier Matching Results
1. FinPilot navigates to **Reconciliation Details** (`/reconciliations/:id`).
2. Notice the itemized match results:
   - **Level 1**: Exact match for `TXN-1001` (100% confidence).
   - **Level 2**: Amount mismatch detected for `TXN-1002` (Internal: ₹80,000 vs Bank: ₹78,500; Variance: ₹1,500).
   - **Unmatched**: Missing internal transaction for `EXT-UNKNOWN-99`.
3. Notice that FinPilot automatically created trackable exception cases!

### Step 5: Open Exception & Launch AI Multi-Agent Investigation
1. Navigate to **Exceptions Queue** (`/exceptions`).
2. Click on case **"EXP-1044"** (Amount Discrepancy of ₹1,500.00).
3. On the **Exception Investigation** page, click **"Launch AI Investigation"**.
4. Watch the 5-agent LangGraph execution pipeline run:
   - `InvestigationAgent` ingests context
   - `EvidenceAgent` gathers transaction gross and bank net settlements
   - `RootCauseAgent` deduces 94.5% probability of payment gateway merchant fee withholding
   - `PolicyValidationAgent` verifies variance is below the ₹50,000 threshold
   - `ResolutionAgent` recommends debiting Fee Expense (5010) and crediting Suspense (1030)

### Step 6: Submit Adjustment Proposal for Human Approval
1. Click **"Submit for Maker-Checker Approval"**.
2. Select Debit Account: `5010 - Interchange & Gateway Fee Expense`.
3. Select Credit Account: `1030 - Reconciliation Suspense Account`.
4. Click **"Route for Approver Authorization"**.
5. Notice the exception transitions to status `PENDING_APPROVAL`.

### Step 7: Test Segregation of Duties (Self-Approval Prevention)
1. Navigate to **Approval Center** (`/approvals`).
2. Find your submitted request. Click **"Review & Decide"**.
3. Notice FinPilot warns:
   `"Separation of Duties Alert: You are logged in as the submitter (analyst). You cannot approve your own request."`
4. If you attempt to force approval, the backend blocks it with `HTTP 403 Forbidden`!

### Step 8: Switch to Finance Approver & Authorize
1. In the top navigation bar, click the **"Approver"** quick persona button (Sarah Jenkins).
2. On request `REQ-5021`, click **"Review & Decide"**.
3. Enter review note: *"Verified against payment gateway monthly schedule."*
4. Click **"Authorize & Execute Adjustment"**.

### Step 9: Verify Deterministic Ledger Posting
1. FinPilot executes the double-entry journal entry and closes the exception!
2. Navigate to **Journal Entries** (`/journals`).
3. Observe the newly posted journal entry `ADJ-EXP-1044`:
   - Debit: `5010 Interchange & Gateway Fee Expense` ₹1,500.00
   - Credit: `1030 Reconciliation Suspense Account` ₹1,500.00
   - Mathematical Balance verified: Debits = Credits = ₹1,500.00!
4. Navigate to **Trial Balance** (`/trial-balance`) to confirm the general ledger remains in equilibrium.

### Step 10: Inspect Immutable Audit Log
1. Click the **"Auditor"** persona button in the header (Marcus Vance).
2. Navigate to **Immutable Audit Logs** (`/audit-logs`).
3. Verify the non-repudiable audit records documenting each step, actor, before/after states, and correlation IDs.
