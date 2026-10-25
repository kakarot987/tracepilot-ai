# TracePilot AI — Complete project guide

> As-built guide based on the repository's `main` branch, 2026-10-25. TracePilot is a **local, synthetic-data portfolio MVP**, not a production observability service or autonomous incident remediator.

## 1. What is TracePilot AI?

TracePilot is an evidence-oriented assistant for investigating *simulated* software incidents. A user selects an incident in a React dashboard; a Java/Spring Boot backend coordinates four logical agent roles using a shared local Ollama model. The investigation gathers authorized read-only evidence from PostgreSQL and optional runbook retrieval through pgvector, records each workflow step, and returns hypotheses and independent Reviewer feedback. It can explicitly report `INSUFFICIENT_EVIDENCE` rather than inventing a diagnosis.

Its core distinction from a chat-only demo is the controlled investigation lifecycle: tools have validation and budgets; evidence has stable identifiers and scoped persistence; claims are reviewed against evidence excerpts; decisions, handoffs and timings are stored; execution ownership is guarded by database leases.

## 2. The user problem

An on-call engineer often has fragmented context: incident metadata, logs, deployments, service metadata, earlier incidents and runbooks. Understanding which observations matter and which explanation is *supported* takes time. TracePilot demonstrates how a guided investigation can organize that process and expose its reasoning artifacts for review.

It does **not** replace incident responders, prove all causal claims, connect to production monitoring systems out of the box, or execute remediation. The seeded corpus is synthetic; any business benefit is a plausible use case, not proven time savings.

## 3. What has been built

| Layer | Technology / implementation | Responsibility |
| --- | --- | --- |
| UI | React 19, TypeScript, Vite; Nginx in Docker | Incident selection, async start, 2.5-second polling, step trace, evidence IDs, hypotheses and Reviewer report |
| REST | Java 21, Spring Boot 4.1.1 | Incident/workflow APIs and validation |
| AI | Spring AI 2.0.1 and local Ollama `qwen3:4b` | Structured triage, evidence-grounded investigation drafts, hypotheses and review |
| Investigation tools | Java allowlisted callbacks | `searchLogs`, `getServiceInfo`, `getRecentDeployments`, `getIncidentHistory`, runbook search |
| Persistence | PostgreSQL, Flyway | Incidents, investigation evidence, workflows, steps, outputs, ownership leases |
| Retrieval | pgvector and local `embeddinggemma` | Versioned synthetic runbook chunk search |
| Operations | Docker Compose, CI, tests | Reproducible local stack and regression verification |
| Evaluation | Synthetic fixtures, Python capture/scoring | Assessment framework; model accuracy coverage remains limited |

The database has 20 distinct seeded incidents after V17. The evaluation documentation currently reports only one reviewed live workflow, a correct abstention. **20 fixtures do not imply 20 validated model predictions.**

## 4. Architecture

See [as-built architecture and reliability flows](TRACEPILOT_ARCHITECTURE.md).

Logical roles, executed sequentially using the same local chat model:

1. **Triage** — classify/structure an incident and the next investigative work.
2. **Investigator** — collect read-only operational evidence, inspect relevant documents where useful, persist evidence IDs and create a bounded draft.
3. **Root Cause** — prepare hypotheses with supporting/contradicting IDs and unresolved questions; observed symptoms are not automatically causal diagnoses.
4. **Reviewer** — examine authorized source excerpts and claims; choose acceptance, bounded follow-up, rejection or insufficient evidence.

At most one focused follow-up can re-run Investigator → Root Cause → Reviewer. A demonstrated database case collected `LOG-DB-001/002/003` on its first pass, and additionally `DEP-DB-001` and `SVC-order-service` on a follow-up. New evidence does not automatically make the root cause conclusive.

## 5. Typical user journey

1. Start local containers and ensure chat and embedding models and indexed runbooks are available as needed.
2. Open the dashboard, select a seeded incident and click **Start investigation**.
3. The backend accepts a persisted asynchronous workflow and returns an ID.
4. The browser polls workflow and step endpoints; refreshing can reconnect using the locally saved ID.
5. After terminal status, the UI loads latest Root Cause / Reviewer outputs and step-specific evidence references.
6. A human reads the *actual* evidence and decides whether the hypothesis is justified. A final `INSUFFICIENT_EVIDENCE` is a valid safe outcome.

The UI's human-approval panel is a **local-only sandbox** for manually drafted proposals. It does not authorize or execute infrastructure changes.

## 6. What a client or user might gain

Potential benefits for a future *authorized* deployment: structured investigation checklists, a shared incident narrative, faster access to documentation, auditability of where a claim came from, easier review across shifts, and fewer premature unsupported statements. Real integration, privacy controls, operational benchmarks and a support model would be required to validate those benefits for a client.

Example: during a database incident, logs may establish connection-acquisition delays while deployment history and service configuration offer context. The system should not conclude that traffic spikes caused saturation if no request-rate evidence was retrieved.

## 7. When this is appropriate — and when not

**Appropriate:** local portfolio demonstrations, investigation workflow experiments, synthetic incident training, API/agent architecture learning, comparison of evidence-first AI approaches.

**Not ready:** feeding real customer secrets/logs; exposing APIs to the public internet; unreviewed production remediation; claiming reliable root-cause accuracy across all 20 scenarios.

Pattern-based redaction and cited-ID provenance are defenses, not guarantees of confidentiality or semantic truth. Timing limits are cooperative for synchronous inference, not hard model cancellation.

## 8. Where to go next

- [Architecture, agent roles and failure flows](TRACEPILOT_ARCHITECTURE.md)
- [Low-level REST API walkthrough](TRACEPILOT_API_WALKTHROUGH.md)
- [Integration guide for other applications](TRACEPILOT_INTEGRATION_GUIDE.md)
- [Business value, testing, security and interview questions](TRACEPILOT_USE_CASES_AND_OPERATIONS.md)
- [Release checklist](RELEASE_CHECKLIST.md), [evaluation](../evaluations/milestone-6-phase-2.md), [project TODO](../TODO.md)

Documentation describes existing source behavior where stated; proposed integration and product ideas are explicitly identified as such.
