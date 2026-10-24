# Milestone 4 — Single-agent vs multi-agent evaluation

> Results are from developer-run Docker/Ollama experiments; the repository review verifies the committed CSV values, not independent runtime execution. HTTP 200 means the API responded successfully, **not** that a supported root cause was established.

## Evaluation definitions

| Metric | Measurement |
| --- | --- |
| Conclusive completion | Completed evidence-supported investigations / total attempts |
| Safe insufficient-evidence outcome | `INSUFFICIENT_EVIDENCE` / attempts |
| HTTP success | HTTP 200 / requests (separate from conclusive completion) |
| Timeout rate | `TIMED_OUT` / attempts; HTTP errors counted separately |
| Median latency | Median request latency; include unsuccessful application results and label separately |
| Evidence citation validity | Valid cited IDs / cited IDs in the corresponding investigation's persisted evidence |
| Root-cause correctness | Compare claims against independently maintained synthetic incident ground truth |
| Unsupported claims | Claims not justified by current incident evidence |
| Follow-up frequency | Multi-agent executions with one or more follow-ups / multi-agent attempts |

## Current post-fix comparison — committed CSV (2026-10-24)

Source: [milestone-4-comparison.csv](milestone-4-comparison.csv). Four synthetic incidents, one attempt per mode and incident, eight total requests.

| Incident | Single-agent status | Single HTTP | Single latency (ms) | Multi-agent status | Multi HTTP | Multi latency (ms) | Multi follow-ups |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: |
| INC-DB-001 | INSUFFICIENT_EVIDENCE | 200 | 11,681 | INSUFFICIENT_EVIDENCE | 200 | 10,527 | 0 |
| INC-PAY-001 | INSUFFICIENT_EVIDENCE | 200 | 9,235 | INSUFFICIENT_EVIDENCE | 200 | 10,356 | 0 |
| INC-RATE-001 | INSUFFICIENT_EVIDENCE | 200 | 9,124 | INSUFFICIENT_EVIDENCE | 200 | 10,679 | 0 |
| INC-MEM-001 | INSUFFICIENT_EVIDENCE | 200 | 9,215 | INSUFFICIENT_EVIDENCE | 200 | 10,438 | 0 |

### Summary

| Metric | Single-agent | Multi-agent |
| --- | ---: | ---: |
| Attempts | 4 | 4 |
| HTTP 200 | 4/4 (100%) | 4/4 (100%) |
| Evidence-supported conclusive completions | 0/4 (0%) | 0/4 (0%) |
| `INSUFFICIENT_EVIDENCE` | 4/4 (100%) | 4/4 (100%) |
| Explicit `TIMED_OUT` outcomes | 0/4 | 0/4 |
| HTTP errors | 0/4 | 0/4 |
| Median request latency | 9,225 ms | 10,482.5 ms |
| Follow-up invocations recorded | Not applicable | 0/4 |

All runs terminated with insufficient evidence. Consequently **citation validity, root-cause accuracy, unsupported-claim rate and accepted Reviewer outcomes cannot be established** from the CSV. The multi-agent median is 1,257.5 ms higher in this small sample, but the attempts may stop before Root Cause/Reviewer; this is not a valid speed comparison of completed four-role investigations.

## Historical baseline — 2026-10-23, before Triage JSON fix

The earlier evaluation had four single-agent HTTP 200 / `INSUFFICIENT_EVIDENCE` runs, while all four multi-agent attempts failed with HTTP 400. Median request latencies were **9,418.5 ms** (single) and **9,236.5 ms** (multi, ending early with HTTP errors). These are historical regression context, not current results.

Earlier live diagnostics identified an empty Triage response and then non-JSON prose. The backend introduced Triage-specific structured output configuration and model-output error classification. Subsequently, a live `INC-DB-001` request returned workflow `08b14cf0-a608-45ca-a354-8f79a2acd71d`, status `INSUFFICIENT_EVIDENCE`, follow-ups 0. The developer reported trace validation passed, but the step rows for that workflow were not provided.

## Outstanding acceptance work — merge blocker

1. Retrieve persisted steps for one **current** multi-agent run (for example `ee67e650-3ac6-4e99-b88a-a51afbd26f8b`) to locate the first insufficient-evidence transition. Check `INVESTIGATOR` JSON output and its child investigation's evidence rows/tool-call count.
2. Verify that a seeded case supplies real operational evidence and that the Investigator records and cites it. Avoid seeding a fabricated answer into the model output.
3. Demonstrate at least one live `TRIAGE → INVESTIGATOR → ROOT_CAUSE → REVIEWER` workflow with evidence-backed Reviewer acceptance and persisted four-stage trace.
4. Repeat each comparison across multiple runs with identical model settings, documented model warm-up state and GPU resource constraints. Report conclusive completion, citations, unsupported claims and latency.
5. Verify Maven, PostgreSQL integration tests, lease-fencing tests and live Ollama execution on the latest application commit. None of these tests were independently executed while authoring this report.

**Assessment:** API handling and safe termination are improved; conclusive end-to-end effectiveness remains unverified, so Milestone 4 merge should remain on hold.
