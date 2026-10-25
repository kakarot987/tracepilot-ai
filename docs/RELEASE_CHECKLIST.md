# Milestone 6 — Phase 6 release acceptance

**Status:** Release candidate preparation, **not a tagged release**. This checklist distinguishes checked-in automation from validations that require the developer's local GPU and PostgreSQL environment.

## Verified by automated CI (at branch head)

- [x] Backend Maven `clean verify` succeeds, including evaluation fixture tests (CI at `0b41510b`).
- [x] Frontend unit tests, TypeScript production build and Playwright mock-API smoke test succeeded on `2ac9aaa9` (rerun at `0b41510b` pending at last check).
- [x] Frontend Docker build and Compose configuration validated in CI on `2ac9aaa9`.

## Must verify locally before v1.0.0

- [ ] Fresh clone and full Docker Compose startup with documented `POSTGRES_PASSWORD`.
- [ ] Ollama models downloaded, loaded and usable after a fresh restart.
- [ ] Flyway migrations applied; 20 synthetic incidents visible through API and dashboard.
- [ ] Read-only tool retrieval, runbook indexing and citation IDs verified end-to-end.
- [ ] At least one fresh multi-agent workflow yields safe terminal state and persisted trace.
- [ ] Browser refresh/reconnect doesn't create a duplicate workflow.
- [ ] PostgreSQL collation mismatch handled through a backed-up/index-rebuild maintenance procedure, if still applicable.
- [ ] Run a full PostgreSQL-enabled integration suite, not only default CI tests.
- [ ] Collect human-annotated real-model evaluation observations; separate FAILED runs from safe abstentions and don't claim unmeasured accuracy.
- [ ] Review the exact contents of local evaluation JSON, logs, screenshots and demo for sensitive information before publishing.
- [ ] Record hardware/RAM/VRAM used, model versions, embedding/index setup and startup time.
- [ ] Choose an appropriate LICENSE and include it.
- [ ] Add screenshots and a 2–3 minute demo link.
- [ ] Document authentication, rate limiting, PII and approval sandbox limitations.
- [ ] Review branch diff and merge; create `v1.0.0` tag only after acceptance.

## Known scope limitations

Human approvals are frontend-local and non-enforcing. No autonomous infrastructure write actions. Secret/PII filtering is pattern-based. Citation provenance is not semantic entailment. Recovery counters are process-local and model timeouts are cooperative, not hard cancellation.

A clean CI run proves source tests and image build, **not** completed fresh-clone local acceptance or production security.

## Fast-track portfolio release option

A portfolio/demo **preview** can be published without completing the full 20-case benchmark, as long as the README prominently discloses the single reviewed abstention case and lack of supported-cause accuracy measurements. This must **not** be called a production-ready release.

Minimum preview acceptance:
- [x] Backend CI passed on `0b41510b`; frontend CI previously green on `2ac9aaa9`.
- [x] One live workflow finished safely with persisted Reviewer/agent trace (developer-observed).
- [x] Offline evaluation reports 1/20 reviewed cases, correct abstention, no measured supported-case accuracy.
- [ ] Confirm branch-head Frontend CI green.
- [ ] Quick dashboard/API smoke check against latest local Docker rebuild.
- [ ] Check the repository, screenshots and demo for secrets/PII; no real incident data.
- [ ] Add one screenshot and a concise demo walkthrough; label as local synthetic demo.
- [ ] Decide the repository license; no default license is assumed.

If those final checks pass, create a clearly labeled preview tag such as `v0.1.0-preview` instead of `v1.0.0`. Keep the larger production-oriented gates open.
