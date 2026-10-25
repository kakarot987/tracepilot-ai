# Milestone 6 — Phase 4: prompt redaction foundation

The `PromptRedactor` now masks common inline credentials (passwords, API keys, access/refresh tokens, bearer tokens) and email addresses in initial incident prompts and Reviewer evidence/hypothesis excerpts before they reach the model. The original evidence in PostgreSQL remains unchanged for local audit and investigation.

**Scope limitation:** the Investigator's subsequent tool result conversation history is still managed by Spring AI's tool calling mechanism and is not yet uniformly redacted. Source data and frontend output are not sanitized by this change. Redaction is pattern based; arbitrary credentials, structured secrets, addresses, phone numbers and other PII may be missed. This is defense in depth, **not** a guarantee that no sensitive data can reach a model.

The system continues to treat logs and retrieved documents as untrusted instructions. The Reviewer still independently validates provenance and its guarded ACCEPT requirements.

## Verification

```powershell
cd tracepilot-backend
.\mvnw.cmd -Dtest=PromptRedactorTest,ReviewerAgentTest test
```

Next security work should enforce redaction on *all* retrieved tool outputs at the tool-result transport boundary, add strict input-size limits and consider log retention/role-based exposure. Avoid storing real secrets in synthetic fixtures.
