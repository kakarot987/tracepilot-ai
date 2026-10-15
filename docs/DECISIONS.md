# Design Decisions / ADR Log

> **Status:** Initial decisions discussed during planning. Decisions can be revisited based on benchmark evidence; no claim of implementation.

## ADR-001 — Create a new standalone project

**Decision:** `tracepilot-ai` is a new repository. Prior Java/AI/ELK repositories may provide ideas or carefully selected reusable snippets but are not a base application.

**Why:** A coherent from-scratch portfolio architecture and visible progress.  
**Tradeoff:** More initial setup work.

## ADR-002 — Java 21 + Spring Boot + Spring AI

**Decision:** Java-first backend and Spring ecosystem; confirm compatible dependency versions during bootstrap.

**Why:** Leverage six years of Java experience and demonstrate production-style engineering.  
**Alternative:** Python agent frameworks, LangChain4j. **Deferred:** LangChain4j comparison if needed.

## ADR-003 — Local-first LLM and vendor-independent gateway

**Decision:** Begin with Ollama and a compact quantized Qwen candidate. Introduce a provider-neutral abstraction; GroqCloud or Gemini may be optional adapters.

**Why:** No required API charges, offline testability.  
**Tradeoff:** Small local models may underperform in reasoning, tool selection, and valid JSON.

## ADR-004 — Modular monolith before microservices

**Decision:** One Spring Boot application, React frontend and PostgreSQL. Logs and incidents are synthetic data sources within the app boundary.

**Why:** Fit 16 GB RAM and keep focus on agent reasoning rather than service infrastructure.  
**Alternative:** Kafka + ELK microservices. Deferred.

## ADR-005 — PostgreSQL + pgvector for RAG

**Decision:** Persist incidents and vector embeddings in PostgreSQL, with separately indexed synthetic runbooks.

**Why:** Simplify deployment and avoid another database.  
**Tradeoff:** Less specialized retrieval tuning than a dedicated vector system; sufficient for MVP.

## ADR-006 — Controlled orchestration before swarm

**Decision:** Separate Triage, Investigator, Root Cause and Reviewer roles with typed shared state, shared local model, sequential calls and explicit bounds.

**Why:** Deterministic control, traceability and safe failure handling.  
**Alternative:** Fully autonomous supervisor/swarm. Deferred until baseline comparison.

## ADR-007 — Read-only tools and human approval

**Decision:** The model may request a fixed allowlist of validated read-only tools. No automatic remediation or write calls.

**Why:** Security and reliable demo behavior. Any future write tool requires explicit approval and authorization.

## ADR-008 — Measure against a single-agent baseline

**Decision:** Evaluate at least 20 seeded incidents, recording root-cause and evidence quality, errors, latency, and abstention; compare multi-agent workflow against a simpler baseline.

**Why:** Additional agents must justify complexity with measurable gains.

## Open decisions

- [ ] Maven or Gradle and exact Spring Boot/Spring AI versions.
- [ ] Default model after local hardware/tool-call benchmark.
- [ ] Embedding dimension, chunking, top-k and retrieval filters.
- [ ] HTTP polling vs server-sent events for progress.
- [ ] Authentication scheme if the demo ever becomes publicly hosted.
- [ ] Exact agent step/time/token budgets after measurement.

## Decision review process

When a decision changes, record date, context, chosen option, rejected alternatives and consequences here. Link relevant PRs where available.
