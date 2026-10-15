# Project Structure (Proposed)

> **Status: Target layout only, not a statement of files already implemented.** Developer owns all code and infrastructure. ChatGPT edits Markdown only.

## Recommended repository tree

```text
tracepilot-ai/
├── README.md
├── TODO.md
├── docs/
│   ├── PROJECT_STRUCTURE.md
│   ├── ARCHITECTURE.md
│   ├── TECH_STACK.md
│   ├── MVP_SCOPE.md
│   ├── DECISIONS.md
│   ├── EVALUATION.md
│   └── CHATGPT_COLLABORATION.md
├── backend/
│   ├── pom.xml                       # or build.gradle(.kts), choose one
│   └── src/
│       ├── main/
│       │   ├── java/com/tracepilot/
│       │   │   ├── TracePilotApplication.java
│       │   │   ├── config/           # Spring AI, providers, security, observability
│       │   │   ├── common/           # shared validation, errors, identifiers
│       │   │   ├── incident/         # API, domain, use cases, persistence
│       │   │   ├── investigation/    # investigation runs, statuses, reports
│       │   │   ├── agent/            # agent role contracts & implementations
│       │   │   │   ├── triage/
│       │   │   │   ├── investigator/
│       │   │   │   ├── rootcause/
│       │   │   │   └── reviewer/
│       │   │   ├── orchestration/    # workflow, step budget, shared state
│       │   │   ├── tools/            # allowlisted read-only AI tools
│       │   │   ├── llm/              # model abstraction & output parsing
│       │   │   ├── rag/              # indexing, chunking, retrieval, citations
│       │   │   └── telemetry/        # logs, metrics and traces
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/     # migrations when introduced
│       └── test/java/com/tracepilot/
│           ├── incident/
│           ├── investigation/
│           ├── agent/
│           ├── tools/
│           ├── orchestration/
│           └── rag/
├── frontend/
│   ├── src/
│   │   ├── app/                      # routing, app bootstrap
│   │   ├── features/
│   │   │   ├── incidents/
│   │   │   ├── investigations/
│   │   │   └── reports/
│   │   ├── components/               # shared UI
│   │   └── lib/                      # API client and helpers
│   └── package.json
├── simulator/
│   ├── incidents/                    # synthetic seed datasets
│   ├── logs/                         # synthetic log fixtures
│   ├── deployments/                  # synthetic deployment metadata
│   └── runbooks/                     # synthetic KB / postmortems
├── evaluations/
│   ├── cases/                        # held-out input fixtures
│   ├── ground-truth/                 # evaluator-only answers (never in agent context)
│   └── results/                      # measured results, once available
└── infrastructure/
    ├── docker-compose.yml
    └── postgres/                     # database bootstrap when needed
```

The tree intentionally illustrates *logical ownership*; create directories incrementally instead of scaffolding everything before Week 1. Paths ending in Java files or configuration files are illustrative and **have not been created**.

## Package responsibilities

| Package | Owns | Should not own |
| --- | --- | --- |
| `incident` | Incident entities, CRUD, input validation, repositories | LLM prompts |
| `investigation` | Runs, status transitions, evidence references, report persistence | Model-specific APIs |
| `agent` | Typed agent role inputs/outputs, role-specific prompts/logic | Direct database mutations |
| `orchestration` | Routing, shared state, bounded retries/steps, lifecycle | Vendor SDKs |
| `tools` | Strictly validated read-only queries, stable evidence IDs | Hidden writes or free-form shell execution |
| `llm` | Chat model gateway, provider selection, structured output handling | Business workflow logic |
| `rag` | Embeddings, document metadata, chunk indexing and retrieval | Evaluation ground-truth answers |
| `telemetry` | Spans, error counts, timing, correlation IDs | Incident decision-making |

Keep Java package boundaries simple: start as a **modular monolith**. A folder is not automatically a separate deployable service. Refactor by capability only once there is real code to justify it.

## Suggested build sequence

| Milestone | Add first |
| --- | --- |
| Week 1 | `backend/`, `incident/`, `llm/`, `config/`, matching tests |
| Week 2 | `simulator/incidents/`, `simulator/logs/`, `tools/`, `investigation/` |
| Week 3 | `simulator/runbooks/`, `rag/`, PostgreSQL/pgvector infrastructure |
| Week 4 | `agent/` roles, `orchestration/`, trace persistence |
| Week 5 | `frontend/` incident/investigation/report screens |
| Week 6 | `evaluations/`, reproducible local launch, final docs and testing |

## Dependency flow

```text
HTTP Controller -> Application Service -> Orchestrator -> Agent roles -> LLM gateway
                                               |
                                               +-> Read-only Tools -> Repositories
                                               +-> RAG retrieval -> pgvector
Persistence and telemetry support the workflow; they do not decide the root cause.
```

Avoid circular package dependencies and direct frontend access to database or model providers. Store evaluation answer keys separately from the synthetic evidence tools that agents can read.

## Naming and design conventions

- Java packages: lowercase, e.g. `com.tracepilot.orchestration`.
- Classes: descriptive role names, not arbitrary `Util` containers.
- Interfaces around model/tool boundaries only where they help testing or provider switching.
- DTOs and persistent entities should be distinct when their shapes or validation differ.
- Stable `incidentId`, `runId`, and `evidenceId` values must appear in traces and reports.
- Separate test fixtures from application data used in prompts.
- Never put credentials, generated model weights, secrets, local database volumes or personal logs in Git.

## Related docs

- [Architecture](ARCHITECTURE.md) — target component behavior and data flow
- [Tech Stack](TECH_STACK.md) — dependency and hardware decisions
- [MVP Scope](MVP_SCOPE.md) — scope and acceptance criteria
- [TODO](../TODO.md) — implementation order
