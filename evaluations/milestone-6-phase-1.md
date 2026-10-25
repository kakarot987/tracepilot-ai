# Milestone 6 — Phase 1: deterministic evaluation baseline

This baseline has **20 deterministic evaluation scenarios derived from five independently seeded synthetic incidents**. The scenarios are evidence-subset views over Flyway V2 (incident catalog) and V4 (log evidence); they are **not 20 new database incidents**. A separate, newly added Flyway V17 migration now meets the 20 distinct-incident fixture count; see the expansion below.

## Gold-label policy

- `OBSERVED_FAILURE` means the error log identifies the immediate failure mechanism. It does **not** prove the upstream root cause.
- `INSUFFICIENT_EVIDENCE` means the supplied info/warning-only subset cannot establish a conclusive failure.
- All cases specify `underlyingRootCause: UNKNOWN_UNDERLYING_CAUSE`. This prevents assigning a known root cause that the existing evidence does not support.
- Evidence IDs remain incident-scoped and are validated against real seed SQL.
- Each incident contributes four variants: full logs, INFO-only, WARN-only, ERROR-only.

## Run fixture validation

From repository root:

```bash
python -m unittest discover -s evaluations -p 'test_*.py'
```

CI runs this validation before the Java test suite.

## What this test measures

The offline tests verify scenario count, unique case IDs, incident/service ownership of citations, and consistency of expected abstentions. They do **not** measure model accuracy, entailment of citations, retrieval quality or run live Ollama calls. A subsequent phase must add runner outputs, per-case graded decisions and independent root-cause evidence before any top-1 accuracy metric is reported.

## Follow-up

Add further corroborating telemetry beyond the new V17 synthetic causal assertions and evaluate actual model outputs. Preserve the original five incident IDs and migrations.

## Expansion: 20 independently seeded incidents

Flyway V17 adds 15 distinct incidents with 45 scoped log records. Combined with the original V2/V4 seeds, PostgreSQL now contains **20 incidents and 60 log records**. The separate `milestone-6-incident-ground-truth.json` scores 12 cases as supported causal fixture diagnoses and 8 as justified abstentions. The earlier 20 evidence-subset cases remain a secondary stress-test set and are not counted as distinct incidents.

The new labels express what the synthetic fixtures assert, not independent real-world verification or observed model performance. Phase 2 must capture actual model predictions and evaluate citation entailment before reporting accuracy. Validation is wired into Backend CI.
