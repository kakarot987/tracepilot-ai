# Evaluation Plan

> **Status: Test strategy only; no evaluation runs or measured results are claimed.**

## Questions to answer

1. Does the investigation identify the seeded root cause?
2. Are claims backed by real retrieved evidence IDs, with correct provenance?
3. Does the system abstain when evidence is insufficient or contradictory?
4. Does the tool-calling loop respect validation, rate limits and step limits?
5. Is a multi-agent system better than one agent on the same inputs, given latency and complexity?

## Dataset design

Create **at least 20 deterministic synthetic incidents** with held-out ground truth and expected evidence IDs. Cover payment timeout, DB pool exhaustion, upstream outage, rate limit, memory pressure, plus misleading/noisy logs, absent evidence and conflicting records.

Avoid leaking ground truth into agent prompts, retrievable runbooks or tool responses. Keep an evaluation-only answer key.

## Measures

| Metric | Method | Target |
| --- | --- | --- |
| Top-1 root-cause accuracy | Correct first hypothesis / cases with knowable cause | Establish baseline, then improve |
| Citation validity | Substantive cited claims referencing existing and relevant evidence | Report value; aim high |
| Unsupported-claim rate | Unsupported substantive claims / reviewed claims | Minimize |
| Appropriate abstention | No-evidence cases flagged as insufficient | Report separately |
| Tool-call success | Valid successful calls / attempted calls | Track by tool |
| Latency | End-to-end p50/p95 and per-agent duration | Benchmark locally |
| Resource usage | Peak RAM/VRAM, model size, runtime | Fit developer laptop |
| Workflow termination | Runs ending within configured budgets | 100% in bounded tests |

No numerical targets are represented as achieved results.

## Test layers

- Unit: request validation, mapping/JSON parsing, tool argument limits, orchestrator transition logic.
- Integration: PostgreSQL tool queries, indexing/retrieval, persistence, model error propagation.
- Security: prompt injection in logs/runbooks, attempts to trigger unapproved tools, secret redaction.
- Resilience: missing data, timeout, interrupted model call, retry exhaustion, bad arguments and malformed output.
- End-to-end: incident selection through trace and report.

## Experiment protocol

1. Freeze an evaluation dataset and model/parameters for a run.
2. Run a single-agent baseline, then the proposed 4-role workflow.
3. Record version, model, context budget, date, hardware, test case IDs and raw metric counts.
4. Re-run suspect failures; separate model stochasticity from tool/data bugs.
5. Document regressions and examples without inventing evaluation outcomes.

## Results log

| Date | Git commit | Model | Dataset count | Root-cause accuracy | Citation validity | p50/p95 latency | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pending | Pending | Pending | Pending | Not measured | Not measured | Not measured | No implementation results yet |

See [Architecture](ARCHITECTURE.md) and [MVP Scope](MVP_SCOPE.md).
