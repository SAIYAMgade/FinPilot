# FinPilot: Security Architecture & Controls

## 1. Threat Model & Trust Boundaries

FinPilot operates under a strict **Zero Direct AI Mutation** security model:
- **Trust Boundary A (Internet / API Gateway)**: Protected by Spring Security filter chains, BCrypt password hashing, and HMAC-SHA256 JWT tokens.
- **Trust Boundary B (AI Agent Perimeter)**: Agents only execute pre-authorized, read-only tools. Agents cannot execute DDL, arbitrary SQL, or post financial journals.
- **Trust Boundary C (Financial Ledger Invariants)**: Double-entry equilibrium is verified with database constraints and serializable transaction isolation.

---

## 2. Segregation of Duties (Maker-Checker Control)

To eliminate insider threat and unauthorized financial adjustments:
- **The Maker**: A `FINANCE_ANALYST` can inspect exceptions, run AI investigations, and construct adjustment proposals.
- **The Checker**: Only a `FINANCE_APPROVER` or `ADMIN` can authorize the proposal.
- **Strict Self-Approval Barrier**: If the logged-in approver's user ID matches `requested_by`, the backend immediately rejects the request with HTTP 403 Forbidden:
  ```java
  if (approvalRequest.getRequestedBy().getId().equals(approver.getId())) {
      throw ApiException.forbidden("Separation of Duties Violation: You cannot approve an adjustment request you initiated");
  }
  ```

---

## 3. Cryptographic Storage & Secrets
- Passwords are salted and hashed using **BCrypt** with cost factor 10.
- Secrets are supplied strictly through environment variables (`JWT_SECRET`, `DB_PASSWORD`, `REDIS_PASSWORD`).
- No cleartext credentials, tokens, or private keys are committed to the codebase.

---

## 4. Immutable Audit Trails
Sensitive operational and financial events create records in the `audit_logs` table recording:
- `entity_type` & `entity_id`
- `action` (`POST`, `APPROVE`, `REJECT`, `REVERSE`, `INVESTIGATE`)
- `performed_by` username
- Unique distributed `correlation_id`
- `before_state` & `after_state` snapshots
