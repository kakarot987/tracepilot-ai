# TracePilot AI

**Local-first, evidence-grounded incident investigation** built with Java 21, Spring Boot, Spring AI, PostgreSQL/pgvector, Ollama, React and Docker Compose.

> **Status:** Working local portfolio MVP; not production-ready. Investigation results are hypotheses requiring human verification. Synthetic incidents only; no autonomous infrastructure write actions.

## What works

- 20 seeded synthetic incidents, scoped log evidence and read-only investigation tools.
- Four-role workflow: Triage → Investigator → Root Cause → Reviewer, with at most one focused follow-up.
- Async workflow submission, persisted execution steps, stable evidence IDs, report views, live polling and refresh recovery.
- Local runbooks and pgvector retrieval, plus a review-only approval sandbox (decisions are **not** persisted or enforced).
- Bounded tool/model requests, workflow leases and expiry recovery.
- Unit, integration and frontend browser smoke tests, with deterministic evaluation fixtures and an offline scoring harness.

## Local quickstart

Prerequisites: Docker Desktop with Compose, sufficient RAM for PostgreSQL/Ollama/Java, and a GPU-capable Ollama setup if using the configured GPU service. Set `POSTGRES_PASSWORD` in `infrastructure/.env` (do not commit it). The Docker Compose stack assumes NVIDIA GPU availability for the Ollama container.

```powershell
git clone https://github.com/kakarot987/tracepilot-ai.git
cd tracepilot-ai
git switch feature/milestone-6
cd infrastructure
docker compose up -d --build
docker compose ps
```

Open http://localhost:5173. The Nginx frontend forwards `/api` to the Spring Boot backend. Spring Boot is also available at http://localhost:8080. The local Ollama server must contain the configured chat model `qwen3:4b` and embedding model; model downloads and embedding index setup may be required before end-to-end investigation and runbook retrieval work.

Inspect the seeded incidents:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/incidents
```

Choose an incident, start an investigation and inspect the persisted trace and Reviewer report. The workflow may legitimately return `INSUFFICIENT_EVIDENCE`: HTTP success or completed steps do not demonstrate a proven root cause.

For development without the frontend container:

```powershell
cd tracepilot-frontend
npm ci
npm run dev
```

## Tests and evaluations

```powershell
python -m unittest discover -s evaluations -p "test_*.py"
cd tracepilot-backend
.\mvnw.cmd clean verify
cd ..\tracepilot-frontend
npm ci
npm test
npm run build
npm run test:e2e
```

For browser E2E tests, install Playwright Chromium with `npx playwright install chromium` if needed. Browser smoke tests mock API calls; they are **not** live Ollama tests.

Evaluation assets: [20 seeded incident gold labels](evaluations/milestone-6-incident-ground-truth.json), [offline scoring script](evaluations/score_workflows.py) and [capture instructions](evaluations/milestone-6-phase-2.md). The currently committed local observations do not establish root-cause accuracy across the 20-case benchmark.

## Architecture and documentation

Start with the [complete project guide](docs/TRACEPILOT_GUIDE.md), then explore:

- [As-built architecture, agents and reliability flows](docs/TRACEPILOT_ARCHITECTURE.md)
- [REST APIs and low-level execution walkthrough](docs/TRACEPILOT_API_WALKTHROUGH.md)
- [Integration into another Java/Spring Boot project](docs/TRACEPILOT_INTEGRATION_GUIDE.md)
- [Client value, operations, testing, security and interview guide](docs/TRACEPILOT_USE_CASES_AND_OPERATIONS.md)

Additional references: [original conceptual architecture](docs/ARCHITECTURE.md), [evaluation plan](docs/EVALUATION.md), [Milestone checklist](TODO.md), [Milestone 6 security notes](evaluations/milestone-6-phase-4.md).

## Limitations and release gates

- No production authentication or authorization, hardened request limits, or production remediation approval enforcement.
- Initial prompt, Reviewer and tool response redaction are best-effort and do not guarantee complete secret/PII protection; persisted evidence and UI output are not comprehensively sanitized.
- Citation IDs establish provenance, **not** semantic truth.
- Synchronous model calls are cooperative-deadline bounded, not forcibly cancelled.
- Recovery counters are in-memory only; no durable operational telemetry.
- A PostgreSQL collation mismatch was observed locally; any index rebuild should be backed up and planned separately.
- Clean-clone reproducibility, full PostgreSQL integration tests, 20-case live evaluation, screenshots/demo, licensing and release tag are not yet verified.

**Do not tag v1.0.0 or claim production readiness until release gates are reviewed.**
