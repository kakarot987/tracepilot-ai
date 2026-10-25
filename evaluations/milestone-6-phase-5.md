# Milestone 6 — Phase 5: recovery and observability

The 30-second PostgreSQL recovery sweep is already responsible for closing workflows beyond their deadline. This phase adds:

- A cumulative counter for recovered expired workflows.
- A separate counter and structured error log for failed recovery sweeps.
- Regression tests that verify successful, empty and failed sweeps.

These in-process counters reset on backend restart; they are **not** exported Prometheus/Micrometer metrics, durable telemetry, or evidence of recovered runs in production. Recovery already existed; this change improves visibility, not the underlying expiry algorithm.

Check backend logs for `Recovered ... expired workflows` or `Workflow recovery sweep failed`. The latter should be investigated, not ignored. Future work should include Actuator metrics, an exception/failure-reason audit, integration tests with an expired PostgreSQL lease, and dashboard status sampling.
