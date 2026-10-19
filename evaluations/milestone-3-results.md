# Milestone 3 — Retrieval Evaluation Results

**Branch:** `feature/milestone-3`  
**Embedding model:** `embeddinggemma`  
**Vector database:** PostgreSQL 17 + pgvector  
**Evaluation dataset:** `milestone-3-retrieval.csv`

## Retrieval metrics

| Metric | Target | Measured |
|---|---:|---:|
| Recall@3 | ≥ 70% | Pending |
| Recall@5 | ≥ 90% | Pending |
| MRR@5 | Record baseline | Pending |

## Safety and correctness

| Test | Result |
|---|---|
| Valid persisted citations accepted | Pending |
| Fabricated runbook citations rejected | Pending |
| Mixed valid/invalid citations rejected | Pending |
| Empty retrieval handled | Pending |
| Conflicting historical documents handled | Pending |
| Prompt-injection behavioral evaluation | Pending |
| Existing Milestone 2 regression tests | Pending |

## Known limitations

- Similarity scores are not calibrated probabilities of relevance.
- Top-k retrieval alone cannot reliably reject irrelevant queries.
- Citation ID validation does not establish semantic claim support.
- Prompt-injection resistance requires repeated behavioral evaluation.
- Embedding and chat requests remain subject to cooperative investigation deadlines rather than guaranteed hard cancellation.

## Conclusion

Record the benchmark measurements, failed cases, and corrective actions before marking Milestone 3 complete.