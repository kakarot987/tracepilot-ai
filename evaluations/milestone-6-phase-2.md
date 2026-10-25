# Milestone 6 — Phase 2 offline workflow evaluation

The runner scores **captured predictions**, not imagined model outputs. It never calls Ollama.

## Input contract

Save a JSON array in a local file such as `evaluations/local-predictions.json` (do not commit sensitive logs):

```json
[
  {
    "incidentId": "INC-CACHE-001",
    "workflowId": "actual-uuid-from-run",
    "status": "COMPLETED",
    "latencyMs": 15620,
    "predictedRootCause": "CACHE_TTL_ZERO",
    "citedEvidenceIds": ["LOG-CACHE-002", "LOG-CACHE-003"],
    "toolCalls": 3,
    "successfulToolCalls": 3
  }
]
```

All fields must come from an observed workflow or explicit human annotation. **Do not infer `predictedRootCause` from ground truth**. Set it to `null` if no unambiguous supported diagnosis was returned; do not count a symptom description as a root cause. The workflow status alone is not an accepted causal diagnosis. Tool counts are optional; only include verified counts.

## Run

From repository root:

```powershell
python evaluations/score_workflows.py --predictions evaluations/local-predictions.json --output evaluations/local-report.json
python -m unittest discover -s evaluations -p "test_*.py"
```

The report includes case coverage, top-1 accuracy among observed supported-cause cases, safe abstention among observed abstention cases, citation ID validity, overlap with labeled causal evidence IDs, optional tool-call success and median observed latency. Denominators are **observed**, so always display coverage; a sample of one cannot establish whole-benchmark accuracy. Citation-ID overlap is not proof of semantic entailment.

## Database collation warning

A PostgreSQL collation-version warning means persisted collation metadata differs from the container's current libc/ICU version. Back up the database before maintenance, then identify indexes depending on the default collation and rebuild affected indexes (e.g., REINDEX DATABASE in a maintenance window) **before** using `ALTER DATABASE tracepilot REFRESH COLLATION VERSION`. Refreshing metadata alone does not rebuild affected indexes. Avoid automatic changes to the developer's local database.
