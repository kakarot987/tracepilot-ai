### Timeout and execution guarantees

TracePilot uses cooperative execution deadlines rather than forcibly interrupting active model inference.

- **Agent deadlines:** Checked before and after synchronous model and tool calls. Results arriving after expiration are rejected.
- **Workflow deadline:** Four minutes. Successful workflow transitions are rejected after expiration.
- **Execution leases:** Sixty-second database leases with fencing tokens protect workflow ownership.
- **Heartbeat:** Periodically renews active leases while ownership remains valid.
- **Recovery:** Expired workflows are reconciled through persisted-state recovery.
- **HTTP timeouts:** Separate from workflow deadlines. A client or transport timeout does not necessarily cancel inference immediately.

A running Ollama request may continue after its logical deadline. Deadline enforcement prevents late results from being accepted but does not guarantee immediate termination of inference.