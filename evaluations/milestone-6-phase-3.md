# Milestone 6 — Phase 3: adversarial evidence tests

The Reviewer tests now exercise three cases using mocked model outputs:

1. A log with role-spoofing instructions is provided as explicitly untrusted source content; the resulting safe abstention is preserved.
2. A runbook claims that it can authorize `ACCEPT`; the Reviewer rejects historical-only support for a current incident.
3. A log instructs the model to cite a nonexistent ID; the provenance validator rejects that fabricated citation.

These are **deterministic validation tests**, not demonstrations that the live LLM reliably ignores all prompt injections. A model can still follow malicious instructions and return structurally valid, misleading claims; syntactic citation checks cannot prove semantic entailment. Future work should include live adversarial evaluations, retrieval-stage injections and semantic review.

Run `cd tracepilot-backend; ./mvnw -Dtest=ReviewerAgentTest test` (PowerShell: `./mvnw.cmd -Dtest=ReviewerAgentTest test`).
