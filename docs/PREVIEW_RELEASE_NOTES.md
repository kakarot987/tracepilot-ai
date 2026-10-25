# TracePilot AI — v0.1.0-preview

**Status:** Portfolio/demo preview only — not production-ready.

TracePilot AI is a local-first, Java 21/Spring Boot, PostgreSQL/pgvector, Ollama and React multi-agent incident investigation prototype operating against **synthetic data**.

## Highlights

- Four-role bounded workflow: Triage, Investigator, Root Cause and Reviewer, including one focused follow-up.
- 20 synthetic seeded incidents, with scoped read-only evidence and local runbook retrieval.
- Async investigation execution with persisted trace, evidence references, diagnostic hypotheses and Reviewer feedback.
- React investigation dashboard with polling and reconnect after refresh.
- Evidence provenance checks, bounded tool calls, basic prompt/tool-result secret redaction, and workflow lease recovery.
- Deterministic evaluation fixture tests and an offline scoring tool that distinguishes unreviewed captures, technical failures and scored outcomes.

## Evaluation disclosure

One **human-reviewed** live workflow has been committed for `INC-PAY-001`. It reached `INSUFFICIENT_EVIDENCE`, matching its insufficient-evidence gold outcome, with an observed elapsed time of 42,526 ms. This is **1/20 benchmark coverage**, **1/1 reviewed safe abstention**, and **no measurement** of supported-cause diagnostic accuracy. No aggregate root-cause accuracy claim is warranted.

## Known limitations

- No production authentication/authorization, complete rate limiting, hardened PII treatment or actual remediation approval enforcement.
- Approval actions in the UI are illustrative and do not execute or persist changes.
- Citation IDs prove provenance but not semantic support; model conclusions require human review.
- Pattern-based redaction may miss secrets or PII, and raw persisted evidence remains accessible through local application infrastructure.
- Workflow timeout/recovery and monitoring do not provide production-grade availability guarantees.
- Fresh-clone startup and GPU/model setup depend on local host configuration and have not been independently reproduced as a release test.
- PostgreSQL local collation maintenance may be necessary depending on the host/container environment.

## Installation and demo

Use the [README](../README.md) and [release acceptance checklist](RELEASE_CHECKLIST.md). Start the Compose stack locally with configured secrets and Ollama models, open the dashboard, select a seeded incident, and inspect the final workflow trace and Reviewer rationale.

## Release decision

Before publishing a GitHub Release or tagging `v0.1.0-preview`:

- Confirm repository licensing and publication permissions.
- Review generated artifacts for secrets/PII.
- Add a representative dashboard screenshot or short local-demo recording.
- Confirm the documented quickstart from a clean clone.
- Record the passing branch-head CI run.

Do not use `v1.0.0` until broader benchmark coverage, security and reliability gates are explicitly met.
