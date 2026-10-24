# Milestone 4 — Evaluation results

> Evidence status: historical baseline measured by the developer on 2026-10-23; subsequent live workflow result reported on 2026-10-24. Automated test pass and latest trace verification were reported by the developer, not independently executed here. These results do **not** demonstrate a successful four-agent root-cause conclusion.

## Evaluation metrics

| Metric | Measurement |
| --- | --- |
| Completion rate | Conclusive successful runs divided by attempts; report insufficient-evidence separately |
| Timeout rate | `TIMED_OUT` runs divided by attempts; distinguish HTTP errors |
| Median latency | Median elapsed time across attempts, noting failed attempts |
| Evidence citation validity | Cited IDs present in the investigation's recorded evidence |
| Root-cause correctness | Compare with independently stored synthetic ground truth |
| Unsupported claims | Claims without sufficient operational evidence |
| Follow-up frequency | Multi-agent runs that reserve a follow-up investigation |

## Baseline: 2026-10-23 (before Triage JSON-mode fix)

| Incident | Single-agent status | Single latency (ms) | Multi-agent status | Multi HTTP | Multi latency (ms) |
| --- | --- | ---: | --- | ---: | ---: |
| INC-DB-001 | INSUFFICIENT_EVIDENCE | 18,950 | HTTP_ERROR | 400 | 9,316 |
| INC-PAY-001 | INSUFFICIENT_EVIDENCE | 9,433 | HTTP_ERROR | 400 | 9,322 |
| INC-RATE-001 | INSUFFICIENT_EVIDENCE | 9,246 | HTTP_ERROR | 400 | 9,157 |
| INC-MEM-001 | INSUFFICIENT_EVIDENCE | 9,404 | HTTP_ERROR | 400 | 9,102 |

- Single-agent: 4/4 HTTP 200 responses, but 0/4 conclusive investigations; all returned `INSUFFICIENT_EVIDENCE`.
- Multi-agent: 0/4 HTTP 200 responses, 4/4 HTTP 400 responses. These were not classified as timeouts.
- Median elapsed time across these attempts: single-agent **9,418.5 ms**; multi-agent **9,236.5 ms**. Multi-agent latency here measures an early failure, not completed investigations.
- Citation validity, root-cause correctness, unsupported claims, and follow-up frequency cannot be compared meaningfully from this baseline.

## Live recheck: 2026-10-24 (after targeted Triage output fixes)

The developer reported this HTTP result from `POST /api/v1/incidents/INC-DB-001/workflows`:

```text
workflowId    : 08b14cf0-a608-45ca-a354-8f79a2acd71d
status        : INSUFFICIENT_EVIDENCE
followUpsUsed : 0
rootCause     :
review        :
```

This confirms that the live endpoint returned a parsed workflow result instead of the earlier HTTP 400/502 failures. `INSUFFICIENT_EVIDENCE` is a safe terminal outcome; it is not a verified root-cause finding. The developer subsequently reported that the workflow trace verification **passed**, but the actual trace rows, step statuses and durations were not shared, so they are not reproduced or inferred here.

### Related diagnosis and fixes

- A prior Docker log captured a blank Triage response (`length=0`), then `AgentOutputValidationException` and HTTP 502.
- A later response was nonempty but invalid JSON, beginning with prose (`Unrecognized token 'We'`), leading to HTTP 400 from an `IllegalArgumentException` path.
- The developer subsequently implemented targeted Triage model-output changes and reported the above successful HTTP-level invocation.
- Follow-up acceptance still requires a full evidence-supported `TRIAGE → INVESTIGATOR → ROOT_CAUSE → REVIEWER` live execution and a post-fix repeated comparison.

## Outstanding evaluation

1. Capture the actual step trace for workflow `08b14cf0-a608-45ca-a354-8f79a2acd71d` and identify where insufficient evidence was determined.
2. Repeat single-agent and multi-agent runs over the same seeded incidents using the corrected backend; preserve HTTP failures and timeouts.
3. Score conclusions against synthetic ground truth and persisted evidence, not merely HTTP 200.
4. Document hardware/model settings, model warm-up state and latency variance.
