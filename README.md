<p align="center">
  <img src="docs/images/finpilot-hero.svg" width="100%" alt="FinPilot Enterprise Financial Operations Platform" />
</p>

# FinPilot ⚡
### Enterprise Financial Operations, Double-Entry Ledger & Agentic Reconciliation Platform

[![CI Pipeline](https://github.com/SAIYAMgade/FinPilot/actions/workflows/ci.yml/badge.svg)](https://github.com/SAIYAMgade/FinPilot/actions/workflows/ci.yml)
[![Java 17/21](https://img.shields.io/badge/Java-17%20%2F%2021-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Python 3.12+](https://img.shields.io/badge/Python-3.12%2B-3776AB?logo=python&logoColor=white)](https://python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![React 18](https://img.shields.io/badge/React-18.3-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

---

## 1. Executive Overview & Problem Statement

Modern financial operations teams process millions of transactions across disparate payment gateways, banking partners, and enterprise ledgers. Traditional reconciliation platforms suffer from two major flaws:
1. **Brittle Rule Engines**: They generate thousands of unmatched exceptions that require slow manual human investigation.
2. **Uncontrolled LLM Hallucinations**: Naive AI chatbots cannot guarantee financial correctness, lack auditability, and risk catastrophic unauthorized balance modifications.

**FinPilot** is an enterprise-grade financial operations platform combining **deterministic financial engineering** with **controlled, auditable multi-agent AI investigation**:
- **Deterministic Core**: Double-entry ledger postings, mathematical invariant checks ($\sum \text{Debits} = \sum \text{Credits}$), idempotency deduplication, and 4-tier statement matching are executed exclusively by deterministic Java code.
- **Controlled Multi-Agent AI**: Specialized LangGraph agents investigate discrepancies, cite verified evidence, deduce root causes, check compliance policies, and propose adjustments.
- **Zero Autonomous AI Mutation**: AI agents have strictly read-only tools. Under no circumstances can an AI agent modify ledger balances or bypass human authorization.
- **Maker-Checker Governance**: All financial adjustments mandate two-party segregation of duties. Self-approval is cryptographically and logically prohibited.

---

## 2. System Architecture

<p align="center">
  <img src="docs/images/architecture-graph.svg" width="100%" alt="FinPilot Architecture Diagram" />
</p>

```mermaid
flowchart TB
    subgraph Client ["Client & Presentation Layer"]
        FE["React 18 + Vite + TypeScript\n(Tailwind CSS / Lucide / Recharts)"]
    end

    subgraph APILayer ["Core Backend (Spring Boot 3.3 - Java 17/21)"]
        GW["REST API Gateway & Security (JWT / RBAC)"]
        TXN["Transaction Ingestion & Idempotency"]
        LEDGER["Double-Entry Ledger (Accounts, Journals, Trial Balance)"]
        RECON["Deterministic 4-Tier Reconciliation Engine"]
        EXC["Exception Management Hub"]
        APPR["Maker-Checker Approval Center"]
        OUTBOX["Transactional Outbox Relayer"]
    end

    subgraph AILayer ["AI Investigation Service (Python 3.12+ / FastAPI / LangGraph)"]
        AG_ROUTER["Multi-Agent Orchestrator"]
        subgraph Agents ["Controlled Agent Team"]
            INV["Investigation Agent"]
            EVID["Evidence Agent"]
            RC["Root Cause Agent"]
            POL["Policy Validation Agent"]
            RES["Resolution Agent"]
        end
    end

    subgraph StorageLayer ["Persistence & Messaging Layer"]
        PG[("PostgreSQL 16 (ACID Persistence)")]
        REDIS[("Redis 7 (Idempotency Cache)")]
        KAFKA["Apache Kafka (Domain Events)"]
    end

    FE -->|HTTPS / REST API| GW
    GW --> TXN & LEDGER & RECON & EXC & APPR
    TXN & LEDGER & RECON & EXC & APPR -->|JPA / Flyway| PG
    TXN -->|Idempotency Check| REDIS
    OUTBOX -.->|Reliable CDC Publishing| KAFKA
    EXC -->|Trigger Investigation| AG_ROUTER
    AG_ROUTER --> Agents
    Agents -->|Read-Only Authorized Tools| GW
```

---

## 3. Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Lucide icons, Recharts, Axios |
| **Core Backend** | Java 17/21, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Hibernate, Flyway, Apache Commons CSV, SpringDoc OpenAPI |
| **AI Backend** | Python 3.12+, FastAPI, LangGraph, Pydantic v2, Pytest |
| **Datastores & Messaging** | PostgreSQL 16, Redis 7, Apache Kafka & Zookeeper |
| **DevOps & Cloud** | Multi-stage Docker, Docker Compose, GitHub Actions, AWS ECS Fargate, AWS RDS, AWS S3 |

---

## 4. Pre-Seeded Demo Accounts

FinPilot provides seeded demo accounts for local evaluation with pre-configured chart of accounts, sample transactions, and active exception cases:

| Role | Username | Password | Email | Assigned Capabilities |
|---|---|---|---|---|
| **ADMIN** | `admin` | `Password@123` | `admin@finpilot.io` | Full platform administration, accounts setup, user provisioning |
| **FINANCE_ANALYST** | `analyst` | `Password@123` | `analyst@finpilot.io` | Ingest payments, execute reconciliation, run AI investigations, propose adjustments |
| **FINANCE_APPROVER** | `approver` | `Password@123` | `approver@finpilot.io` | Maker-Checker second-party authorization, approve/reject adjustment proposals |
| **AUDITOR** | `auditor` | `Password@123` | `auditor@finpilot.io` | Read-only inspection of transactions, ledgers, trial balances, and immutable audit logs |

---

## 5. Quickstart Guide

### Option A: Complete Docker Compose (Recommended)
Launch all subsystems (Postgres, Redis, Zookeeper, Kafka, Spring Boot backend, FastAPI AI service, and React frontend) with a single command:

```bash
# 1. Clone repository
git clone https://github.com/SAIYAMgade/FinPilot.git
cd FinPilot

# 2. Start container stack
docker compose up -d --build

# 3. Access applications:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator Health: http://localhost:8080/actuator/health
# AI Service: http://localhost:8000/docs
```

### Option B: Local Standalone Development
You can also run services individually without external infrastructure:

```bash
# 1. Start Java Backend (uses in-memory PostgreSQL-compatible mode with Flyway & Seeder)
cd backend
mvn spring-boot:run

# 2. Start Python AI Service (in a second terminal)
cd ../ai-service
pip install -r requirements.txt
uvicorn main:app --port 8000 --reload

# 3. Start React Frontend (in a third terminal)
cd ../frontend
npm install
npm run dev
# App will open at http://localhost:5173
```

---

## 6. End-to-End Walkthrough Scenario

Follow this guided workflow to evaluate FinPilot's capabilities:

1. **Sign In**: Navigate to `http://localhost:3000/login` and click **"Finance Analyst"** (`analyst` / `Password@123`).
2. **Ingest Transactions**: Go to `/transactions` and ingest a payment with an idempotency key. Notice that re-submitting identical requests returns the original transaction with zero duplicate records.
3. **Execute Statement Reconciliation**: Navigate to `/reconciliations/new`, click **"Download Sample CSV"**, upload the settlement file, and click **"Execute 4-Tier Reconciliation Match"**.
4. **Review 4-Tier Matches**: Inspect the results on `/reconciliations/:id`. Observe Level 1 exact matches, Level 2 amount mismatches, and automatically created exception cases.
5. **Launch AI Multi-Agent Investigation**: Go to `/exceptions`, open case **EXP-1044** (₹1,500 discrepancy), and click **"Launch AI Investigation"**.
6. **Inspect Evidence & Probabilities**: Watch the 5-agent LangGraph pipeline ingest context, cite evidence IDs, deduce root causes (94.5% probability of gateway fee withholding), and verify tolerance thresholds.
7. **Submit Adjustment Proposal**: Click **"Submit for Maker-Checker Approval"** and route the proposal for independent authorization.
8. **Enforce Separation of Duties**: Go to `/approvals` as the analyst and attempt to approve your own proposal. Observe that FinPilot immediately blocks self-approval with `HTTP 403 Forbidden`!
9. **Switch to Approver & Authorize**: Click the **"Approver"** quick persona button (Sarah Jenkins) and click **"Authorize & Execute Adjustment"**.
10. **Verify Ledger Invariant & Audit Trail**: Go to **Journal Entries** (`/journals`) and **Trial Balance** (`/trial-balance`) to confirm the adjustment entry was posted with mathematical equilibrium ($\sum \text{Debits} = \sum \text{Credits}$). Inspect `/audit-logs` for the cryptographic before/after trail.

---

## 7. Synthetic Data Generator (10,000+ Transactions)

FinPilot includes a deterministic synthetic data generator producing over 10,000 realistic transactions with companion external bank settlement records:

```bash
python scripts/generate_synthetic_data.py --count 10000 --seed 42
# Output:
# Generated transactions_10k.csv with 10000 rows.
# Generated bank_statement_10k.csv with 9771 rows.
```

---

## 8. Automated Testing & Verification

Every critical constraint is verified by automated test suites:

```bash
# Run Java Backend JUnit 5 Suite (7 tests, 0 failures)
cd backend && mvn test

# Run Python Multi-Agent Pytest Suite (2 tests, 0 failures)
cd ../ai-service && python -m pytest tests/

# Run Frontend Typecheck & Build (0 errors)
cd ../frontend && npm run build
```

---

## 9. Comprehensive Documentation Sitemap

- [System Architecture & Blueprint](docs/architecture.md)
- [Database Schema & ER Diagrams](docs/database-design.md)
- [REST API Reference & Contracts](docs/api-reference.md)
- [Security & Segregation of Duties](docs/security.md)
- [Automated Testing & Verification Report](docs/testing.md)
- [Docker & AWS Cloud Deployment Guide](docs/deployment.md)
- [10-Step Guided Demo Walkthrough](docs/demo-scenarios.md)

---

## 10. Engineering Portfolio Note & Technical Interview Ready
FinPilot is structured and coded to the standards of senior financial software architecture. It demonstrates:
- Double-entry accounting invariants and concurrency safety
- Transactional Outbox Pattern for distributed event delivery
- Controlled multi-agent AI with bounded read-only tool invocation
- Two-party maker-checker segregation of duties
- Clean, type-safe, and auditable code across Java, Python, and TypeScript.
