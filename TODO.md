# TracePilot AI — Development TODO

> **Goal:** Build a portfolio-ready, Java-first, multi-agent incident investigation platform from scratch. Reuse ideas or snippets from older projects only when useful; keep this a standalone application.
>
> **Constraints:** Free-first development, 16 GB RAM, Intel i7-14700HX, RTX 4050 Laptop GPU (6 GB VRAM). Build and run locally before considering hosting.
>
> **Ownership:** I implement the code and infrastructure. ChatGPT can help plan, review, troubleshoot, and maintain **Markdown documentation only**.

## Definition of done (portfolio v1)

- [ ] A user can create or select a **synthetic** software incident.
- [ ] A Java backend can retrieve actual simulated logs and evidence through read-only tools.
- [ ] A multi-agent workflow produces a root-cause **hypothesis**, not an unsupported assertion.
- [ ] The report cites log IDs, time ranges, and/or runbook passages that support its findings.
- [ ] Users can see each agent's step, tool call, status, and elapsed time.
- [ ] The workflow stops safely when evidence is insufficient, a tool fails, or a step limit is reached.
- [ ] An evaluation set of at least 20 seeded incidents is documented and tested.
- [ ] The whole demo can be started locally with clear setup steps and no paid API requirement.
- [ ] A public README, architecture diagram, screenshots, and short demo video explain the project.

## Milestone 0 — Create the project (Day 1)

- [x] Create a **new** GitHub repository named `tracepilot-ai` (public when ready).
- [x] Add root `.gitignore` and an initial project `README.md` (verified on feature branch).
- [ ] Choose and add an open-source `LICENSE` file (not found on feature branch).
- [x] Choose Maven with Java 21, Spring Boot 4.1.1 and Spring AI 2.0.1 (configured in `tracepilot-backend/pom.xml`; developer reports successful tests).
- [x] Document proposed module boundaries: `backend/`, `frontend/`, `simulator/`, `infrastructure/`, `docs/`, `evaluations/` (see [Project Structure](docs/PROJECT_STRUCTURE.md); implementation pending).
- [x] Define the MVP scope and write down **out-of-scope** items (no autonomous production changes, no real credentials, no expensive infrastructure).
- [ ] Create GitHub Issues for the milestones below; label them `mvp`, `enhancement`, `docs`, and `testing` as appropriate.
- [x] Commit this checklist as `TODO.md` and use it as the source of truth.

**Status (2026-10-16):** Repository, initial README, `.gitignore`, Maven/Java/Spring configuration, structure plan, and scope are verified. Open-source license and milestone GitHub Issues remain outstanding (none found in GitHub issue search).

**Exit criteria:** Organized standalone repository, documented MVP scope, and working local prerequisites; GitHub project-management hygiene remains to be completed.

## GitHub + ChatGPT collaboration

- [x] Connect GitHub to ChatGPT and verify `kakarot987/tracepilot-ai` is readable.
- [x] Document working agreements in [`docs/CHATGPT_COLLABORATION.md`](docs/CHATGPT_COLLABORATION.md).
- [x] Add `README.md` with the one-paragraph MVP problem statement and repository overview.
- [x] Request GitHub-based implementation reviews after completed milestones (Milestones 1 and 2 reviewed).
- [x] Update `TODO.md` and the progress log after verified milestones (continue for future milestones).

**Boundary:** ChatGPT may create or edit Markdown documentation on request; application code, infrastructure, commits of non-Markdown files, and implementation remain the developer's responsibility.

## Milestone 1 — Model setup and first REST API (Week 1)

- [x] Install Ollama and verify GPU use with `nvidia-smi` and `ollama ps`.
- [x] Try a compact quantized model such as `qwen3:4b`; verify performance on the laptop before committing to it.
- [x] Keep initial context size modest (around 4K tokens) and inference concurrency at one; tune after measuring.
- [x] Bootstrap Spring Boot with Spring AI, validation, actuator, and test dependencies.
- [x] Define a provider-independent model abstraction so local Ollama can be swapped later.
- [x] Implement `POST /api/v1/incidents/analyze` with a typed request and response.
- [x] Include `severity`, `category`, `hypotheses`, and `requiresInvestigation` in the response contract.
- [x] Add request validation, structured-output parsing, bounded retries, and meaningful error responses.
- [x] Write unit and integration tests for valid input, invalid input, model failure, and malformed output.
- [x] Verify RAM/VRAM usage for a representative local inference request (developer confirms checked on laptop; exact measurements not recorded in repository).
- [ ] Optional documentation follow-up: add approximate RAM/VRAM numbers and measurement method to a Markdown benchmark note for reproducibility (not a Milestone 1 blocker).

**Status (2026-10-16):** Core Milestone 1 implementation complete. Developer reports clean Maven tests, opt-in live Ollama tests, Docker endpoint verification, and failure-response checks passed. Previous exception-classification regression corrected in source review. RAM/VRAM use was checked by the developer; recording the actual values in the repository remains an optional documentation follow-up.

**Exit criteria:** Met per developer verification: a tested Java endpoint returns structured initial incident hypotheses using a free local model.

## Milestone 2 — Synthetic incident data and tools (Week 2)

- [x] Create a synthetic incident catalog: payment timeout, DB connection exhaustion, upstream outage, rate limit, and memory pressure.
- [x] Generate deterministic logs and service metadata for each seeded incident; record the known root cause separately for evaluation.
- [x] Persist incident metadata and investigation runs (start with PostgreSQL; avoid extra services until needed).
- [x] Implement read-only tools: `searchLogs`, `getServiceInfo`, `getRecentDeployments`, `getIncidentHistory`.
- [x] Apply strict tool argument validation, time-window and row limits, and bounded database queries; retain cooperative investigation deadline checks (not a hard end-to-end timeout).
- [x] Return stable evidence IDs from tools so findings can be traced to source records.
- [x] Build one Investigator Agent that selects and uses tools rather than receiving all data in the prompt.
- [x] Limit tool calls (8), model requests (6), and use a cooperative 90-second investigation deadline; recover stale persisted runs after 120 seconds (not hard cancellation).
- [x] Test no-data, tool-timeout, bad-arguments, and duplicate-request scenarios.

**Status (2026-10-19): COMPLETE — merged into `main`.** Reviewed four read-only tools, deterministic synthetic incident evidence, PostgreSQL/Flyway persistence, evidence IDs, bounded tool-calling Investigator Agent, citation ID checking, and recovery. Developer confirms Maven, PostgreSQL, and live Ollama tests pass and no real secrets remain in Git history. Timeout behavior is documented in [Milestone 2 notes](docs/milestone-2.md); a hard 90-second end-to-end timeout is **not** claimed.

**Exit criteria:** Met based on source review and developer-confirmed runtime tests: the agent retrieves synthetic evidence through Java tools and handles invalid input and failures safely.

Note: TracePilot validates that an investigation's cited evidence identifiers were recorded during its tool execution. This prevents acceptance of missing or unrecognized citations. It does not establish that every factual claim is logically supported by the cited evidence. Semantic claim verification is deferred to the Milestone 4 Reviewer Agent.

## Milestone 3 — RAG and citations (Week 3)

- [x] Create 8–12 realistic, synthetic runbooks and postmortems under `simulator/runbooks/`.
- [x] Set up `pgvector` in PostgreSQL.
- [x] Integrate local `embeddinggemma` embeddings alongside the chat model; keep separate RAM/VRAM measurement as an optional benchmark follow-up.
- [x] Build a repeatable indexing pipeline with document IDs, chunk IDs, source names, and versions.
- [x] Implement semantic retrieval with top-k limits and optional metadata filters.
- [x] Return runbook titles, passages and stable chunk IDs to the Investigator Agent for citation in investigation drafts (UI display is planned for Milestone 5).
- [x] Make the model distinguish **retrieved facts**, **hypotheses**, and **missing evidence**.
- [x] Test irrelevant retrieval, conflicting documents, empty search results, and prompt-injection text embedded in documents.
- [x] Record retrieval quality on a small hand-labeled question set.

**Status (2026-10-19): COMPLETE — merged into `main`.** Source review confirmed ten synthetic runbooks/postmortems, pgvector migrations, local EmbeddingGemma indexing, version-aware document/chunk metadata, bounded semantic search, agent tool integration, citation-ID validation, and adversarial/retrieval tests. The committed [retrieval evaluation](evaluations/milestone-3-results.md) reports Recall@3 = 1.000, Recall@5 = 1.000, and MRR@5 = 0.833 over ten labeled queries; these are developer-reported results, not independently executed in this review. The separate Docker indexer and model-resource benchmarking remain useful reproducibility checks.

**Exit criteria:** Met for the Milestone 3 source-reviewed scope: investigations can retrieve runbook passages with stable citations and identify missing support. Citation validation checks recorded IDs, not the semantic truth of claims; the Reviewer Agent is planned for Milestone 4.

## Milestone 4 — Controlled multi-agent orchestration (Week 4)

- [ ] Define explicit roles: **Triage**, **Investigator**, **Root Cause**, and **Reviewer**.
- [ ] Define typed shared state: incident ID, gathered evidence, hypotheses, open questions, and workflow status.
- [ ] Implement an orchestrator with a clear sequence and conditional branches.
- [ ] Have the Reviewer verify that major claims reference evidence IDs.
- [ ] Permit a bounded follow-up investigation when evidence is insufficient.
- [ ] Prevent infinite loops with limits on steps, reassignments, token usage, and runtime.
- [ ] Persist intermediate steps and statuses for debugging and UI display.
- [ ] Keep agents on one shared local model initially; **do not** load four separate model instances.
- [ ] Compare one-agent and multi-agent output on the same test cases to see if complexity helps.

**Exit criteria:** One investigation executes multiple distinct agent roles with traceable, bounded behavior.

## Milestone 5 — Full-stack dashboard (Week 5)

- [ ] Bootstrap a React + TypeScript frontend.
- [ ] Build pages for incident list, incident detail, investigation progress, and investigation report.
- [ ] Let users start an investigation and view its current status.
- [ ] Show a chronological agent trace with tool calls, returned evidence IDs, timings, and errors.
- [ ] Display hypotheses, confidence qualifiers, cited evidence, and recommended next steps.
- [ ] Add polling or server-sent events for status updates (start with whichever is simpler).
- [ ] Support a human approval step for any proposed **write** action (even if execution remains disabled in MVP).
- [ ] Handle failure, timeout, cancel, and insufficient-evidence states in the UI.

**Exit criteria:** A user can investigate a synthetic incident end to end without calling APIs manually.

## Milestone 6 — Quality, security, and portfolio release (Week 6)

- [ ] Create at least 20 deterministic synthetic incident cases with known causes and expected evidence.
- [ ] Measure root-cause top-1 accuracy, evidence citation correctness, tool-call success, and latency.
- [ ] Capture false positives and cases where the correct behavior is to say “insufficient evidence.”
- [ ] Add prompt-injection tests against logs and retrieved documents; treat external content as untrusted data.
- [ ] Add redaction for secrets and personally identifying information in logs and model input.
- [ ] Add request limits, dependency timeouts, retry policies, and audit-friendly traces.
- [ ] Produce a reproducible local Docker Compose setup; document memory needs and startup order.
- [ ] Verify the project works from a fresh clone on the same laptop.
- [ ] Write `README.md` with problem statement, stack, architecture, prerequisites, quickstart, sample incident, screenshots, limitations, and evaluation results.
- [x] Add initial planning versions of `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, and `docs/EVALUATION.md` (update with implemented details/results later).
- [ ] Record a 2–3 minute demo and add its link to the README.
- [ ] Pin a `v1.0.0` release after a final test and documentation pass.

**Exit criteria:** A stranger can understand, run, test, and evaluate the demo using your documentation.

## Stretch goals — Only after v1

- [ ] Implement an MCP server/client integration for a read-only investigation tool.
- [ ] Run independent retrieval tasks in parallel and compare latency and quality.
- [ ] Experiment with bounded supervisor-led dynamic task delegation (“swarm” behavior).
- [ ] Add GroqCloud as an optional free-tier provider; keep Ollama as the no-cost baseline.
- [ ] Compare models on the same incident evaluation suite; record cost, speed, and accuracy tradeoffs.
- [ ] Integrate a real log backend such as Elasticsearch **only if** it improves the demo enough to justify resource use.
- [ ] Add GitHub issue creation only after explicit human approval and scoped authentication.

## Suggested task order for the very first session

1. [x] Create the repository and commit `TODO.md`.
2. [x] Write a one-paragraph MVP problem statement in `README.md`.
3. [x] Verify Ollama GPU inference with one small model (developer confirmed).
4. [x] Scaffold the Spring Boot backend.
5. [x] Implement the first typed `/api/v1/incidents/analyze` contract.
6. [x] Commit the first working test and example request/response.

## Guardrails for this laptop and free budget

- Prefer **Ollama + one compact quantized model**, sequential agent calls, and a small context window.
- Avoid launching Elasticsearch, Kafka, multiple Java microservices, and multiple loaded LLMs simultaneously in v1.
- Use local Postgres/pgvector and synthetic data. Cloud AI should be an **optional** provider with usage limits.
- Never commit `.env` files, API keys, tokens, raw personal logs, or production incident data.
- Avoid claiming an incident is definitively resolved without evidence and verification.

## Progress log

| Date | Milestone | What changed | Next blocker |
|---|---|---|---|
| 2026-10-15 | 0 | Repository `kakarot987/tracepilot-ai` and `TODO.md` confirmed on `main` | README, project setup and local prerequisites remain |
| 2026-10-15 | 0 | Added GitHub–ChatGPT Markdown collaboration workflow | Follow the review and documentation process |
| 2026-10-15 | 0 | Added README and planning docs for stack, architecture, MVP, design decisions, and evaluation | Choose build tool, verify local model and begin implementation |
| 2026-10-15 | 0 | Documented proposed repository tree and package responsibilities | Developer to scaffold modules incrementally |
| 2026-10-16 | 1 | Reviewed `feature/milestone-1`; exception classification, retries, typed API, tests and Docker integration present; developer confirmed verification checks passed | Merge before Milestone 2; optional numeric RAM/VRAM documentation |
| 2026-10-16 | 0–1 | Re-audited Milestone 0: README, ignore file, Maven and scope confirmed. Developer confirms RAM/VRAM verification; no values published | Add license and optional GitHub milestone issues |
| 2026-10-19 | 2 | `feature/milestone-2` merged into `main`; source review complete and Maven/PostgreSQL/live Ollama checks confirmed by developer | Begin Milestone 3 RAG and pgvector; license and issue housekeeping remain |
| 2026-10-19 | 3 | `feature/milestone-3` merged into `main`; runbook RAG, pgvector, embedding versioning, agent citations, and retrieval evaluation recorded | Begin Milestone 4 controlled multi-agent orchestration |

---

**Current next action:** Create `feature/milestone-4` and implement controlled Triage → Investigator → Root Cause → Reviewer orchestration with typed shared state, evidence-linked hypotheses, bounded follow-up, persisted execution traces, and one shared local model. Preserve read-only tools and cooperative timeout semantics. Milestone 0 license/GitHub Issues and optional hardware benchmark documentation remain housekeeping items. See [ChatGPT collaboration workflow](docs/CHATGPT_COLLABORATION.md).
