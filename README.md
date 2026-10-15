# TracePilot AI

> **Status: Planning / implementation not yet verified.** This is a new, standalone portfolio project, not an extension of an older repository.

**TracePilot AI** is a Java-first, local-first multi-agent incident investigation platform. Users will create or select **synthetic** microservice incidents; an AI workflow will inspect bounded, read-only log and service tools, consult runbooks using retrieval-augmented generation (RAG), and produce an evidence-linked root-cause **hypothesis**, proposed next steps, and an observable agent trace. The platform will not autonomously change production systems.

## Why build it?

Demonstrate senior backend and agentic-AI engineering together: Spring Boot APIs, reliable tool calling, shared workflow state, RAG, multi-agent orchestration, persistence, tracing, evaluation, and a React UI. It should remain usable on a 16 GB RAM / RTX 4050 6 GB laptop without a paid AI API.

## Planned stack

Java 21; Spring Boot + Spring AI; local Ollama with a compact Qwen model; PostgreSQL + pgvector; local embeddings (candidate: EmbeddingGemma); React + TypeScript; Docker Compose; JUnit 5 and Testcontainers; Micrometer/OpenTelemetry. An optional GroqCloud adapter can be added later. **Versions/models are candidates, not validated selections.**

See [Tech Stack](docs/TECH_STACK.md) and [Architecture](docs/ARCHITECTURE.md).

## Planned MVP user flow

1. Select a seeded synthetic incident (e.g. payment timeout).
2. Start an investigation.
3. Orchestrator coordinates triage, evidence gathering, root-cause hypotheses, and review.
4. Tools retrieve *actual seeded records*, not fabricated tool results.
5. UI displays progress, stable evidence IDs, uncertainties, and a proposed resolution plan.
6. Evaluation compares predictions to separately recorded ground truth.

## Project documentation

| Document | Purpose |
| --- | --- |
| [TODO.md](TODO.md) | Canonical tasks, milestones, and progress |
| [MVP Scope](docs/MVP_SCOPE.md) | Features, exclusions, acceptance criteria |
| [Architecture](docs/ARCHITECTURE.md) | Components, data flows, agent lifecycle |
| [Tech Stack](docs/TECH_STACK.md) | Technology choices and hardware constraints |
| [Design Decisions](docs/DECISIONS.md) | Architectural reasoning and deferred choices |
| [Evaluation](docs/EVALUATION.md) | Quality metrics and test plan |
| [ChatGPT Collaboration](docs/CHATGPT_COLLABORATION.md) | Rules for documentation-only assistance |

## Quickstart

**Not available yet.** Setup instructions, commands, screenshots, and tested prerequisites will be added after the developer implements and verifies the first working version. See [TODO.md](TODO.md) for the next milestone.

## Safety and limitations

Synthetic data only, no hardcoded credentials, no external production tools with write permissions. Treat logs and retrieved documentation as untrusted input. An LLM hypothesis is not a verified diagnosis. Cloud free tiers may change; the project must work locally without an API subscription.

## Ownership

The developer writes and tests all code and infrastructure. ChatGPT may review implementation and create/update **Markdown files only**, when requested.
