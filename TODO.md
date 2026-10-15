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

- [ ] Create a **new** GitHub repository named `tracepilot-ai` (public when ready).
- [ ] Add a `.gitignore`, an open-source license of choice, and an initial project README.
- [ ] Choose Maven or Gradle and lock down Java 21 and a compatible Spring Boot / Spring AI combination.
- [ ] Decide the initial module boundaries: `backend/`, `frontend/`, `simulator/`, `infrastructure/`, `docs/`, `evaluations/`.
- [ ] Define the MVP scope and write down **out-of-scope** items (no autonomous production changes, no real credentials, no expensive infrastructure).
- [ ] Create GitHub Issues for the milestones below; label them `mvp`, `enhancement`, `docs`, and `testing` as appropriate.
- [ ] Commit this checklist as `TODO.md` and use it as the source of truth.

**Exit criteria:** Empty but organized standalone repository, documented MVP scope, and working local prerequisites.

## Milestone 1 — Model setup and first REST API (Week 1)

- [ ] Install Ollama and verify GPU use with `nvidia-smi` and `ollama ps`.
- [ ] Try a compact quantized model such as `qwen3:4b`; verify performance on the laptop before committing to it.
- [ ] Keep initial context size modest (around 4K tokens) and inference concurrency at one; tune after measuring.
- [ ] Bootstrap Spring Boot with Spring AI, validation, actuator, and test dependencies.
- [ ] Define a provider-independent model abstraction so local Ollama can be swapped later.
- [ ] Implement `POST /api/v1/incidents/analyze` with a typed request and response.
- [ ] Include `severity`, `category`, `hypotheses`, and `requiresInvestigation` in the response contract.
- [ ] Add request validation, structured-output parsing, bounded retries, and meaningful error responses.
- [ ] Write unit and integration tests for valid input, invalid input, model failure, and malformed output.
- [ ] Record local latency and approximate RAM/VRAM use for one representative request.

**Exit criteria:** A tested Java endpoint returns structured initial incident hypotheses using a free local model.

## Milestone 2 — Synthetic incident data and tools (Week 2)

- [ ] Create a synthetic incident catalog: payment timeout, DB connection exhaustion, upstream outage, rate limit, and memory pressure.
- [ ] Generate deterministic logs and service metadata for each seeded incident; record the known root cause separately for evaluation.
- [ ] Persist incident metadata and investigation runs (start with PostgreSQL; avoid extra services until needed).
- [ ] Implement read-only tools: `searchLogs`, `getServiceInfo`, `getRecentDeployments`, `getIncidentHistory`.
- [ ] Apply strict input schemas, time-window limits, row limits, and timeouts to every tool.
- [ ] Return stable evidence IDs from tools so findings can be traced to source records.
- [ ] Build one Investigator Agent that selects and uses tools rather than receiving all data in the prompt.
- [ ] Limit maximum tool calls, execution time, and total steps per investigation.
- [ ] Test no-data, tool-timeout, bad-arguments, and duplicate-request scenarios.

**Exit criteria:** The agent retrieves real synthetic evidence with Java tool calls and handles failure safely.

## Milestone 3 — RAG and citations (Week 3)

- [ ] Create 8–12 realistic, synthetic runbooks and postmortems under `simulator/runbooks/`.
- [ ] Set up `pgvector` in PostgreSQL.
- [ ] Try a local embedding model (for example, `embeddinggemma`), checking its resource usage separately from the chat model.
- [ ] Build a repeatable indexing pipeline with document IDs, chunk IDs, source names, and versions.
- [ ] Implement semantic retrieval with top-k limits and optional metadata filters.
- [ ] Display cited document titles/passages in generated answers.
- [ ] Make the model distinguish **retrieved facts**, **hypotheses**, and **missing evidence**.
- [ ] Test irrelevant retrieval, conflicting documents, empty search results, and prompt-injection text embedded in documents.
- [ ] Record retrieval quality on a small hand-labeled question set.

**Exit criteria:** Investigations cite relevant runbooks and decline to claim support when none exists.

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
- [ ] Add `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, and `docs/EVALUATION.md`.
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

1. [ ] Create the repository and commit `TODO.md`.
2. [ ] Write a one-paragraph MVP problem statement in `README.md`.
3. [ ] Verify Ollama GPU inference with one small model.
4. [ ] Scaffold the Spring Boot backend.
5. [ ] Implement the first typed `/api/v1/incidents/analyze` contract.
6. [ ] Commit the first working test and example request/response.

## Guardrails for this laptop and free budget

- Prefer **Ollama + one compact quantized model**, sequential agent calls, and a small context window.
- Avoid launching Elasticsearch, Kafka, multiple Java microservices, and multiple loaded LLMs simultaneously in v1.
- Use local Postgres/pgvector and synthetic data. Cloud AI should be an **optional** provider with usage limits.
- Never commit `.env` files, API keys, tokens, raw personal logs, or production incident data.
- Avoid claiming an incident is definitively resolved without evidence and verification.

## Progress log

| Date | Milestone | What changed | Next blocker |
|---|---|---|---|
| YYYY-MM-DD | 0 | Initial project checklist created | Repository not yet created |

---

**Current next action:** Create `tracepilot-ai` on GitHub, add this `TODO.md`, and begin Milestone 0. Keep the scope focused on an evidence-backed incident investigator, not an unrestricted autonomous production agent.
