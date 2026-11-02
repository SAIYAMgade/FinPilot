# FinPilot: REST API Reference & Contract Specification

FinPilot APIs conform to standard REST conventions and are documented via OpenAPI 3.0 at `/swagger-ui.html` and `/v3/api-docs`.

---

## 1. Authentication & Session Endpoints

### Authenticate Operator
`POST /api/v1/auth/login`
```json
// Request
{
  "usernameOrEmail": "analyst",
  "password": "Password@123"
}

// Response (200 OK)
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 86400000,
    "user": {
      "id": "usr-88a10",
      "username": "analyst",
      "email": "analyst@finpilot.io",
      "fullName": "Senior Finance Analyst",
      "roles": ["FINANCE_ANALYST"]
    }
  }
}
```

---

## 2. Transaction Management Endpoints

### Ingest Transaction
`POST /api/v1/transactions`
```json
// Request
{
  "externalReference": "TXN-9981",
  "sourceSystem": "PAYGATE-IN",
  "amount": 50000.00,
  "currency": "INR",
  "transactionType": "PAYMENT",
  "idempotencyKey": "idemp-cust-9981",
  "description": "Enterprise customer checkout settlement"
}
```

### Search & Filter Transactions
`GET /api/v1/transactions?status=POSTED&page=0&size=20&sortBy=createdAt&sortDir=desc`

### Bulk Import Transactions (CSV)
`POST /api/v1/transactions/import` (Multipart file: `file`)

---

## 3. Double-Entry General Ledger Endpoints

### Create Ledger Account
`POST /api/v1/accounts`
```json
{
  "accountNumber": "1040",
  "accountName": "Treasury Money Market Account",
  "accountType": "ASSET",
  "currency": "INR"
}
```

### Post Balanced Journal Entry
`POST /api/v1/ledger/journals`
```json
{
  "referenceId": "SETTLE-OCT-01",
  "description": "Monthly merchant clearing payout",
  "currency": "INR",
  "autoPost": true,
  "lines": [
    {
      "accountId": "acc-1010",
      "debitAmount": 5000.00,
      "creditAmount": 0.00,
      "description": "Debit cash clearing"
    },
    {
      "accountId": "acc-1020",
      "debitAmount": 0.00,
      "creditAmount": 5000.00,
      "description": "Credit receivables"
    }
  ]
}
```

### Offset / Reverse Posted Journal
`POST /api/v1/ledger/journals/{id}/reverse?reason=Operational+correction`

### Generate Real-Time Trial Balance
`GET /api/v1/ledger/trial-balance`

---

## 4. Reconciliation Engine Endpoints

### Upload External Bank Statement
`POST /api/v1/reconciliations/upload` (Multipart file: `file`)

### Execute 4-Tier Matching Algorithm
`POST /api/v1/reconciliations/{id}/execute?dateToleranceDays=2`

### Get Run Audit Report
`GET /api/v1/reconciliations/{id}/report`

### Export Results to CSV
`GET /api/v1/reconciliations/{id}/export`

---

## 5. Exceptions & AI Investigation Endpoints

### List Exceptions
`GET /api/v1/exceptions?status=OPEN&severity=HIGH`

### Trigger Multi-Agent AI Investigation
`POST /api/v1/ai/investigations`
```json
{
  "exceptionId": "exc-1044"
}
```

---

## 6. Maker-Checker Approval Center Endpoints

### Submit Adjustment Proposal
`POST /api/v1/approvals/proposals`
```json
{
  "exceptionId": "exc-1044",
  "investigationId": "inv-8812",
  "proposedAction": "POST_LEDGER_ADJUSTMENT",
  "adjustmentAmount": 1500.00,
  "debitAccountId": "acc-5010",
  "creditAccountId": "acc-1030",
  "rationale": "Reconcile payment gateway interchange fee variance"
}
```

### Authorize or Reject Request
`POST /api/v1/approvals/{id}/decide`
```json
{
  "decision": "APPROVED",
  "comments": "Verified against gateway monthly fee schedule"
}
```
*(Enforces strict separation of duties: Submitter cannot approve their own proposal)*
