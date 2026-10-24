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

## Live seven-step workflow — 2026-10-24 (separate from comparison CSV)

This is a subsequent **developer-executed** local Docker/Ollama demonstration, not one of the eight paired comparison rows. The workflow response and step/output APIs were shared during re-review; the backend was not remotely executed during documentation review.

| Field | Observed result |
| --- | --- |
| Incident | `INC-DB-001` (`order-service`) |
| Workflow ID | `bfc65394-1ee6-47a6-8204-586763be8456` |
| Workflow status | `INSUFFICIENT_EVIDENCE` |
| Follow-ups consumed | 1 of maximum 1 |
| Root Cause | Returned hypotheses with `sufficientEvidence=false` |
| Final Reviewer output | `FOLLOW_UP` (not `ACCEPT`) |
| Latest Investigator run ID | `2f3cf15d-2b45-4b61-b8df-2ddc9e6510fe` |
| Latest Investigator status | `COMPLETED`, `toolCallsUsed=1` |
| Latest Investigator evidence IDs | `LOG-DB-001`, `LOG-DB-002`, `LOG-DB-003` |

The user-provided persisted steps API returned all seven steps with `COMPLETED` status:

| Step | Role | Status | Duration (ms) |
| ---: | --- | --- | ---: |
| 1 | TRIAGE | COMPLETED | 1,747 |
| 2 | INVESTIGATOR | COMPLETED | 9,480 |
| 3 | ROOT_CAUSE | COMPLETED | 2,751 |
| 4 | REVIEWER | COMPLETED | 2,642 |
| 5 | INVESTIGATOR | COMPLETED | 9,124 |
| 6 | ROOT_CAUSE | COMPLETED | 2,185 |
| 7 | REVIEWER | COMPLETED | 2,569 |

Evidence ID provenance: these three `LOG-DB-00*` IDs exist in the repository's seeded `V4__seed_log_evidence.sql` for this incident. The shared latest Investigator output contains the three evidence IDs and a one-tool-call count. This establishes working evidence acquisition/recording in the live trace, **not** independent verification that every drafted factual claim is substantively supported. The full investigation evidence endpoint response and raw source excerpts were not independently re-fetched for this documentation entry.

The final Reviewer response was: `FOLLOW_UP`, because connection acquisition delays were observed but the cause of saturation or an elevated request rate was not established. Its unresolved questions asked what caused saturation and whether the request rate was high. The workflow correctly terminated as insufficient evidence after exhausting the one permitted follow-up, rather than forcing a conclusion.

### What this demonstration establishes and does not establish

- **Demonstrated:** a live, persisted, ordered four-role sequence, a bounded Reviewer-directed second pass, recorded log evidence IDs, per-step timings and a safe terminal state.
- **Not demonstrated:** a supported final `ACCEPT` decision; conclusive root-cause accuracy; the effectiveness of the follow-up in retrieving *new* evidence. Both passes in the current implementation search the same incident-centered log window.
- **Quality concern:** the displayed Investigator draft begins with self-referential procedural prose (`We are given an incident ID...`) rather than a concise final incident report. The validator verifies the presence/provenance of citations but does not by itself establish that the narrative's individual claims are supported.
- **Acceptance impact:** this **does meet** the milestone's narrow four-role traceability/boundedness demonstration, but **does not meet** the stronger re-review request for one evidence-supported Reviewer-accepted conclusion. Keep the merge recommendation conditional on reviewer-defined acceptance criteria and full tests.

## Historical baseline — 2026-10-23, before Triage JSON fix

The earlier evaluation had four single-agent HTTP 200 / `INSUFFICIENT_EVIDENCE` runs, while all four multi-agent attempts failed with HTTP 400. Median request latencies were **9,418.5 ms** (single) and **9,236.5 ms** (multi, ending early with HTTP errors). These are historical regression context, not current results.

Earlier live diagnostics identified an empty Triage response and then non-JSON prose. The backend introduced Triage-specific structured output configuration and model-output error classification. Subsequently, a live `INC-DB-001` request returned workflow `08b14cf0-a608-45ca-a354-8f79a2acd71d`, status `INSUFFICIENT_EVIDENCE`, follow-ups 0. The developer reported trace validation passed, but the step rows for that workflow were not provided.

## Remaining validation for re-review

1. Obtain one truly evidence-supported, Reviewer-`ACCEPT`ed **live** case. Distinguish a directly observed symptom (JDBC connection acquisition failures) from its unproven upstream cause (traffic volume, held connections, deployment changes).
2. Make focused follow-up retrieval responsive to the Reviewer's unresolved questions instead of repeating exactly the same log query. Preserve authorized read-only tools, actual persisted evidence, and the four-tool follow-up limit.
3. Test the deterministic `searchLogs` callback path for recorded IDs, missing data, evidence persistence failures, deadline/budget accounting, and unsupported citations. Existing handoff/persistence and acceptance-mock tests do not replace this.
4. Re-run `TRACEPILOT_POSTGRES_TEST=true` Maven `clean verify` after the current changes. A passing full-suite result on this revision was not supplied for this report. Keep unit and PostgreSQL integration evidence separate from live Ollama evidence.
5. Re-run the paired comparison on the **current** evidence-first version. Record hardware, model warm-up, per-agent/total latency, follow-up frequency, ground-truth accuracy, valid and substantively supported citations, and unsupported claims. Do not mix this separately executed seven-step demonstration into the historical eight-run CSV.

**Assessment:** The multi-agent orchestration, state persistence and bounded follow-up path are **live-demonstrated**. Safe abstention is evidenced. Full evidence-backed Reviewer acceptance and robust comparative effectiveness remain open; merger depends on the actual reviewer exit criteria.
