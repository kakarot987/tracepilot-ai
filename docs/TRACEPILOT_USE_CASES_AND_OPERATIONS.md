# TracePilot AI — Client value, safety, evaluation and interview guide

> This document separates **implemented synthetic-demo behavior** from **potential commercial applications**. TracePilot is not a production incident-management platform.

## Who might use a hardened version?

- **On-call/SRE engineers:** gather a chronological evidence record, inspect competing hypotheses and request further investigation.
- **Platform teams:** standardize how runbooks, deployment context and incident evidence are presented.
- **Support and incident managers:** review an auditable summary of what was observed, what is hypothesized and what remains unanswered.
- **Training teams:** practice incident diagnosis on synthetic cases with known labels and safe-abstention examples.

These are potential users, not reported adopters.

## Example scenario: database connection failures

An incident in the seeded `order-service` dataset shows database connection acquisition delays. TracePilot's Investigator can collect timestamped `LOG-DB-*` records. A Reviewer-directed second pass has been observed to acquire additional `DEP-DB-001` deployment and `SVC-order-service` service metadata. The user can inspect seven persisted workflow steps.

**What that proves:** evidence retrieval, scoped storage, ordered four-agent flow and a bounded follow-up. **What it does not prove:** that a traffic spike, code regression or deployment caused the saturation. An `INSUFFICIENT_EVIDENCE` conclusion may be the responsible result.

## Potential business outcomes (not measured ROI)

| Business need | Potential contribution | What must be tested |
| --- | --- | --- |
| Reduce time spent gathering context | Single view of incident records, logs, runbooks and step outputs | Human time-to-triage vs existing workflow |
| Improve handovers | Persisted evidence IDs and Reviewer rationale | Completeness and usefulness scored by operators |
| Reduce unsupported conclusions | Reviewer rejects uncited/unsupported hypotheses and can abstain | False-positive rate and human-reviewed citation entailment |
| Build repeatable incident learning | Seeded cases and evaluated failure paths | Coverage across realistic failure categories |
| Preserve operational accountability | Per-step status/duration and evidence provenance | Access controls, retention and audit verification |

Avoid claiming reduced MTTR or quantified cost savings without an actual study.

## Evaluation status and how to interpret it

The Milestone 6 repository includes **20 distinct seeded incident fixtures** and an offline workflow scorer. The committed local report previously covered **one reviewed live run**: `INC-PAY-001` correctly returned insufficient evidence, taking 42,526 ms. That is a **1/20 coverage demonstration**, not a 20-case accuracy result. Supported-cause top-1 accuracy was not measured in that report.

The scorer distinguishes:
- technical failure from diagnostic outcome;
- correctly justified abstention from a supported-cause prediction;
- cited-ID validity/overlap from true semantic support;
- annotation-required captures from human-reviewed labels.

Before claiming reliability, run all cases under recorded hardware/model settings, inspect source excerpts per claim, annotate actual model outputs without copying gold labels, and report coverage and uncertainty.

## Safety and security: current versus required

**Current foundations:** read-only tools, validated arguments, tool/model limits, bounded follow-up, workflow leases/fencing, source/citation scoping, prompt-injection tests, partial pattern-based redaction and explicit abstention.

**Still required for real data:** authentication, authorization and tenant boundaries, comprehensive source/UI redaction, input and output size governance, secure telemetry ingestion, retention/deletion policies, request throttling, independently tested prompt-injection robustness, operational alerting and real human-controlled remediation gates.

The UI approval sandbox does **not** persist or authorize actions. The model runs locally by design, but local hosting by itself is not a substitute for access control.

## Reliability guarantees you can explain

- Workflow state is durable in PostgreSQL; a browser session is not the execution owner.
- Heartbeat and fenced leases guard against stale executors continuing to change workflow state.
- A scheduled job reconciles expired workflows; local recovery counters reset after restart.
- The workflow has a logical four-minute deadline. The Investigator's standard deadline is 90 seconds. An active Ollama inference request might finish after the logical deadline; deadline checks prevent late acceptance, not immediate cancellation.
- A completed API call is not equivalent to a Reviewer-approved diagnosis.

## Testing guide

| Area | Repository artifacts |
| --- | --- |
| Java unit and integration tests | `tracepilot-backend/src/test/`; opt-in PostgreSQL integration profile |
| Frontend behavior | Vitest + React Testing Library; Playwright mocked browser smoke test |
| RAG retrieval | `evaluations/milestone-3-results.md` |
| Evaluation fixtures and scoring | `evaluations/milestone-6-incident-ground-truth.json`, `score_workflows.py` |
| Safety/security tests | Reviewer validation, adversarial evidence cases, redaction unit tests |
| Release readiness | `docs/RELEASE_CHECKLIST.md`, `docs/PREVIEW_RELEASE_NOTES.md` |

Browser smoke tests that mock API data do not validate a live Ollama conclusion. Default Maven CI is not a substitute for full PostgreSQL-enabled integration testing.

## Interview questions with strong answers

**Why four agents rather than one prompt?** To separate incident classification, evidence gathering, hypothesis construction and skeptical review. This makes responsibilities and outputs observable, though extra inference can increase latency.

**Why PostgreSQL/pgvector?** It keeps relational workflow/evidence integrity and vector retrieval in one local stack, reducing infrastructure overhead for a laptop-first demo.

**Why evidence IDs?** To tie agent statements to run-scoped records and make later human verification possible. ID validity is not entailment.

**Why database leases and fencing?** To prevent two executors or a stale worker from simultaneously updating a workflow as if both owned it.

**How do you prevent hallucination?** You cannot guarantee zero hallucinations. You constrain tool access, cite recorded evidence, inspect excerpts and reject unsupported conclusions; the system must still allow abstention.

**How do timeouts work?** Cooperative checks around synchronous calls with persisted terminal states and recovery; model inference is not forcibly interrupted.

**Why not Kafka/Elasticsearch yet?** The synthetic dataset and local workload do not justify another service. A real integration should follow validated scale and operational requirements.

**What would you improve next?** Human-scored 20-case supported-cause evaluation, robust data security, live incident connectors, authenticated APIs, source-passage UI and release reproducibility.

## Client presentation: honest summary

“TracePilot AI is a local-first demonstration of how a controlled AI workflow can support incident investigations. It gathers read-only evidence, records traceable hypotheses, requests one targeted follow-up and safely reports uncertainty. To use it with your production observability stack we would first develop secure connectors, validate diagnostic accuracy against your incidents, and add authentication, privacy and operational controls.”

More: [main guide](TRACEPILOT_GUIDE.md) · [integration](TRACEPILOT_INTEGRATION_GUIDE.md).
