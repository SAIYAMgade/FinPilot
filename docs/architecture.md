# FinPilot: System Architecture & Design Blueprint

## 1. Architectural Philosophy

FinPilot bridges the gap between **deterministic financial accounting** and **autonomous AI-driven investigation**. The system guarantees mathematical ledger invariants while empowering operations teams with controlled AI agents to explain discrepancies and formulate audit-ready corrections.

### The Five Invariants
1. **Mathematical Invariant**: In double-entry posting, $\sum \text{Debits} = \sum \text{Credits}$ is enforced at the domain, application, and database levels. Unbalanced transactions cannot be committed.
2. **AI Mutation Prohibition**: AI agents possess read-only tool privileges. No LLM or agent can directly mutate account balances, change ledger states, or post journal lines.
3. **Idempotency Guarantee**: Ingestion endpoints require an idempotency key. A retry of the same payload returns the original result with 0 side-effects; conflicting parameters on the same key trigger an HTTP 409 Conflict.
4. **Segregation of Duties (Maker-Checker)**: An adjustment proposal submitted by a Finance Analyst must be authorized by an independent Finance Approver. Self-approval is blocked by authorization filters.
5. **Deterministic Multi-Tier Matching**: Reconciliation matching strictly employs four deterministic levels of validation. Discrepancies generate trackable exception records.

---

## 2. Component Topology

```mermaid
flowchart TB
    subgraph UI ["Presentation Layer"]
        REACT["React 18 SPA (Vite + TypeScript + Tailwind)"]
    end

    subgraph BACKEND ["Core Operations Service (Java 17/21 - Spring Boot 3.3)"]
        AUTH["Auth & RBAC (JWT/BCrypt)"]
        TXN["Transaction Ingestion & Idempotency"]
        LEDGER["Double-Entry Ledger Engine"]
        RECON["Deterministic 4-Tier Matcher"]
        EXC["Exception Lifecycle Hub"]
        APPR["Maker-Checker Approval Engine"]
        OUTBOX["Transactional Outbox Relayer"]
    end

    subgraph AI ["AI Investigation Service (Python 3.12+ FastAPI)"]
        LG["LangGraph Multi-Agent Orchestrator"]
        INV_AG["Investigation Agent"]
        EVID_AG["Evidence Agent"]
        RC_AG["Root Cause Agent"]
        POL_AG["Policy Validation Agent"]
        RES_AG["Resolution Agent"]
    end

    subgraph DATA ["Persistence & Event Infrastructure"]
        PG[("PostgreSQL 16 (Flyway Migrations)")]
        REDIS[("Redis 7 (Idempotency Cache)")]
        KAFKA["Apache Kafka (Domain Event Topics)"]
    end

    REACT -->|REST / JWT| BACKEND
    BACKEND -->|JPA / ACID| PG
    BACKEND -->|Idempotency Cache| REDIS
    OUTBOX -->|Outbox Events| KAFKA
    EXC -->|Trigger Investigation| LG
    LG --> INV_AG & EVID_AG & RC_AG & POL_AG & RES_AG
```

---

## 3. Asynchronous Messaging & Transactional Outbox Pattern

To eliminate dual-write hazards between relational persistence and Kafka event dispatching, FinPilot employs the **Transactional Outbox Pattern**:
1. When a transaction changes state or an adjustment is approved, the domain event is serialized to JSON and persisted into the `outbox_events` table within the same atomic database transaction.
2. An asynchronous scheduler (`OutboxPublisherService`) polls pending records every 5,000ms.
3. Events are published to Kafka topics (`transactions-topic`, `reconciliation-topic`, `approvals-topic`).
4. Upon successful Kafka delivery acknowledgment, the outbox record status updates to `PUBLISHED`.
5. If Kafka is temporarily unreachable, exponential backoff retries preserve event delivery without impacting core API latency.

---

## 4. Multi-Agent AI Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Analyst as Finance Analyst
    participant Core as FinPilot Core API
    participant AI as LangGraph Engine
    actor Approver as Finance Approver

    Analyst->>Core: Trigger AI Investigation (Case #EXP-1044)
    Core->>AI: POST /api/v1/investigate
    Note over AI: LangGraph Workflow Initiates
    AI->>AI: Step 1: InvestigationAgent (Context Ingestion)
    AI->>AI: Step 2: EvidenceAgent (Authoritative Record Lookup)
    AI->>AI: Step 3: RootCauseAgent (Hypothesis & Probability)
    AI->>AI: Step 4: PolicyValidationAgent (Threshold Matrix Check)
    AI->>AI: Step 5: ResolutionAgent (Draft Ledger Adjustment Payload)
    AI-->>Core: Structured Pydantic InvestigationReport
    Core-->>Analyst: Display Verified Facts, Root Cause, Recommendation
    Analyst->>Core: Submit Adjustment Proposal for Approval
    Core->>Approver: Notify Pending Approval
    Approver->>Core: Authorize Proposal (Approved)
    Note over Core: Deterministic Ledger Service executes Journal Posting
    Core->>Core: Commit Immutable Audit Trail
```
