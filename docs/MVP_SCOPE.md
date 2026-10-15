# MVP Scope & Acceptance Criteria

> **Status: Product requirements, not implemented features.**

## Problem

Investigating backend failures requires correlating logs, service configuration, deployments and runbooks. TracePilot AI demonstrates how carefully bounded tool-using agents can assemble evidence and explain *probable* root causes for simulated incidents.

## Personas and use cases

A backend engineer selects a synthetic incident (payment timeout, DB pool exhaustion, upstream outage, rate limiting, memory pressure). They initiate investigation, watch evidence gathering, inspect alternative hypotheses and receive recommended **human-executed** next steps.

## In scope for v1

- Standalone new repo / Spring Boot backend and React frontend.
- Seeded, deterministic synthetic incident catalog and read-only Java tools.
- A baseline structured LLM analysis endpoint.
- Retrieved runbooks/postmortems using local embeddings and pgvector.
- Four logical agent roles: Triage, Investigator, Root Cause, Reviewer; bounded orchestration.
- Persist run and step statuses; display trace, evidence references and final report.
- Graceful failure on insufficient evidence, tool timeout, malformed output and exhausted budget.
- At least 20 evaluation incidents, including cases where the correct outcome is **insufficient evidence**.
- Local-first reproducibility without paid APIs, documented hardware limitations.

## Out of scope for v1

- Live production logs or private repositories; real credentials or PII.
- Autonomous production remediation, shell commands, deployments, issue creation or unapproved writes.
- Distributed autonomous swarms, Kafka, Elasticsearch, several microservices, expensive hosting.
- Training or fine-tuning foundation models.
- Guaranteed diagnostic correctness or numerical confidence without calibration.

## Demonstration scenario

Given a synthetic payment-service timeout and a recent deployment, the investigator calls relevant evidence tools, retrieves dated log events and optional runbook chunks, proposes connection-pool exhaustion as a **hypothesis**, and distinguishes observed evidence from inference. Reviewer rejects unsupported statements. UI shows the investigation trace and action suggestions.

## Definition of done

- [ ] Fresh clone can be run locally following tested documented instructions.
- [ ] At least one seeded incident completes the full workflow from UI.
- [ ] Every substantive report claim points to valid evidence IDs or is explicitly marked speculative.
- [ ] Tool inputs and agent step budgets are bounded and tested.
- [ ] Failed/no-evidence workflows terminate clearly rather than fabricating a conclusion.
- [ ] A 20+ case evaluation suite records accuracy, citation validity, tool success and latency.
- [ ] README contains architecture, screenshots, limitations and a short demo link.

For work breakdown see [TODO.md](../TODO.md). Check boxes only against verified working behavior.
