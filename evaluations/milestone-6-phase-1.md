# Milestone 6 — Phase 1: deterministic evaluation baseline

This baseline has **20 deterministic evaluation scenarios derived from five independently seeded synthetic incidents**. The scenarios are evidence-subset views over Flyway V2 (incident catalog) and V4 (log evidence); they are **not 20 new database incidents** and do not satisfy the stronger TODO requirement for 20 independently seeded incidents.

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

Expand Flyway fixtures to at least 20 distinct, independently grounded synthetic incidents (not arbitrary duplicate copies of existing rows). Add supporting operational evidence sufficient to discriminate causal hypotheses and include cases designed for abstention. Preserve the original five incident IDs and migrations.
