### Investigation timeout and recovery semantics

TracePilot Milestone 2 uses three separate mechanisms to limit investigation execution and recover abandoned runs.

**1. Cooperative investigation deadline — 90 seconds**

`InvestigationBudget` establishes a monotonic 90-second deadline. `BoundedModelRunner` checks this deadline before and after model calls and tool execution.

The deadline is cooperative. It does not interrupt a model request or tool invocation already executing synchronously.

**2. HTTP transport timeout**

The backend configures an HTTP connection timeout of 5 seconds and a read timeout of 90 seconds.

During integration testing, a `ReadTimeoutException` was observed when an Ollama request exceeded the previously configured read timeout.

A transport timeout limits individual HTTP operations. It does not guarantee that an entire multi-turn investigation completes within 90 seconds, particularly when retries occur.

**3. Stale investigation recovery — 120 seconds**

`InvestigationRecoveryJob` identifies persisted investigations that remain `RUNNING` beyond 120 seconds and marks them `TIMED_OUT`.

The job executes at application startup and periodically, with a scheduled fixed delay of 30 seconds between executions.

Recovery protects the persisted investigation lifecycle and enables subsequent investigations of the same incident. It does not cancel an Ollama inference request already in progress.

**Guaranteed behavior**

- A maximum of eight evidence-tool executions is configured per investigation.
- A maximum of six model requests is configured per investigation.
- Completed database runs cannot be transitioned to another terminal state through the normal lifecycle update.
- Stale `RUNNING` investigations can be recovered after application restarts.

**Current limitation**

Milestone 2 does not guarantee a hard 90-second end-to-end execution timeout.

Enforcing strict cancellation across model requests, retries, and tool execution is deferred to a future reliability-hardening task.