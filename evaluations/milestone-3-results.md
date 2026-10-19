# Milestone 3 — Retrieval Evaluation Results

**Branch:** `feature/milestone-3`  
**Embedding model:** `embeddinggemma`  
**Vector database:** PostgreSQL 17 + pgvector  
**Evaluation dataset:** `milestone-3-retrieval.csv`

## Retrieval metrics

| Metric | Target | Measured |
|---|---:|---:|
| Recall@3 | ≥ 70% | 1.000 |
| Recall@5 | ≥ 90% | 1.000 |
| MRR@5 | Record baseline | =0.833 |

## Safety and correctness

| Test | Result  |
|---|---------|
| Valid persisted citations accepted | Passed  |
| Fabricated runbook citations rejected | Passed |
| Mixed valid/invalid citations rejected | Passed |
| Empty retrieval handled | Passed |
| Conflicting historical documents handled | Passed |
| Prompt-injection behavioral evaluation | Passed |
| Existing Milestone 2 regression tests | Passed |

## Known limitations

- Similarity scores are not calibrated probabilities of relevance.
- Top-k retrieval alone cannot reliably reject irrelevant queries.
- Citation ID validation does not establish semantic claim support.
- Prompt-injection resistance requires repeated behavioral evaluation.
- Embedding and chat requests remain subject to cooperative investigation deadlines rather than guaranteed hard cancellation.

## Conclusion

Record the benchmark measurements, failed cases, and corrective actions before marking Milestone 3 complete.