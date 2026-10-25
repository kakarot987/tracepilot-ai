# TracePilot AI — API reference and low-level walkthrough

> Grounded in `WorkflowController.java` and `tracepilot-frontend/src/api.ts` on `main`, 2026-10-25. Example IDs are illustrative; response fields reflect application DTOs. The `/api/v1` prefix is assumed throughout.

## 1. Principal endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/incidents` | List seeded incidents for the dashboard |
| POST | `/incidents/analyze` | Initial structured incident analysis (Milestone 1) |
| POST | `/incidents/{incidentId}/workflows/async` | Persist and queue a bounded workflow, return 202 |
| POST | `/incidents/{incidentId}/workflows` | Legacy synchronous orchestration |
| GET | `/workflows/{workflowId}` | Persisted status, progress and failure reason |
| GET | `/workflows/{workflowId}/steps` | Chronologically persisted agent steps |
| GET | `/workflows/{workflowId}/steps/{stepId}/output` | Output for *one owned step* |
| GET | `/workflows/{workflowId}/outputs/{role}` | Latest persisted output for a role |
| GET | `/investigations/{investigationId}/evidence` | Recorded evidence references for an investigation run |

The frontend deliberately calls `/workflows/async`; do not mistake the synchronous POST for the default UI path.

## 2. POST async workflow — controller to database

Request:

```http
POST /api/v1/incidents/INC-DB-001/workflows/async
Accept: application/json
```

Typical immediate result (**202 Accepted**, not a diagnosis):

```json
{"workflowId":"02b5f9ee-8c34-4ae7-bb49-4a501b49fa16","status":"PENDING"}
```

Low-level call path:

1. `WorkflowController.startAsync` uses `IncidentRepository.findById` and returns 404 if unknown.
2. `AsyncWorkflowService.submit` tries to acquire one of two semaphore slots. Capacity rejection raises `RejectedExecutionException`, translated to 503 and `WORKFLOW_CAPACITY` by the controller.
3. `WorkflowExecutionStarter.start` persists workflow metadata while acquiring a fenced lease. An ownership token distinguishes the current executor.
4. The service submits `orchestrator.execute(lease)` to a fixed two-thread executor, then returns the workflow UUID.
5. The worker executes agent roles and writes durable steps/outputs. Dispatch rejection after persistence attempts `WorkflowPersistenceService.failPendingDispatch` for the owned pending workflow.
6. Exceptions and deadlines use failure/safe-terminal paths; **HTTP 202 only means submission**, not that a root cause has been established.

Operational implication: a client should record the returned UUID and poll; it should *not* automatically retry a POST after a transport uncertainty, because it may have succeeded server-side.

## 3. GET workflow and polling

```http
GET /api/v1/workflows/02b5f9ee-8c34-4ae7-bb49-4a501b49fa16
```

Shape based on `WorkflowRun`:

```json
{
  "id": "02b5f9ee-8c34-4ae7-bb49-4a501b49fa16",
  "incidentId": "INC-DB-001",
  "status": "RUNNING",
  "stepsUsed": 2,
  "followUpsUsed": 0,
  "currentRole": "INVESTIGATOR",
  "startedAt": "2026-10-25T10:00:00Z",
  "completedAt": null,
  "failureReason": null
}
```

The UI polls every 2.5 seconds until one of `COMPLETED`, `INSUFFICIENT_EVIDENCE`, `FAILED`, `TIMED_OUT`, `CANCELLED`. `PENDING`/`RUNNING` are **not** final. `INSUFFICIENT_EVIDENCE` is a meaningful safety result, not a successful causal determination.

## 4. GET agent steps and outputs

```http
GET /api/v1/workflows/{workflowId}/steps
GET /api/v1/workflows/{workflowId}/steps/{stepId}/output
GET /api/v1/workflows/{workflowId}/outputs/REVIEWER
```

A step record includes `id`, `workflowId`, `stepNumber`, `agentRole`, `status`, `durationMs`, `errorCode` and `outputSummary`. A step output includes `stepId`, `workflowId`, `agentRole`, `schemaVersion`, serialized `json`, and optionally `investigationRunId`.

**Ownership check:** before reading a step output, `WorkflowController.stepOutput` checks that the supplied step ID belongs to the requested workflow. Unknown workflows and absent outputs return 404.

The frontend reads the JSON output and, if it references an investigation run, calls the evidence endpoint. Evidence references report `investigationId`, `evidenceId`, `evidenceType`, `recordedAt`. They identify stored sources; they are not themselves the full source contents.

The *latest role output* endpoint matters after a follow-up, because a role can appear more than once in the trace. A client seeking *every* pass must retrieve each step by its own ID.

## 5. The four agent APIs internally

There are no separately hosted REST services for Triage, Investigator, Root Cause or Reviewer. They are Java components invoked by the orchestrator. Their responsibilities:

- **Triage:** structured response with classification/urgency and validation.
- **Investigator:** a persisted investigation run, bounded read-only tools, evidence recording, citation validation and draft creation.
- **Root Cause:** hypotheses with `description`, `supportingEvidenceIds`, `contradictingEvidenceIds`, `confidence`, `openQuestions`, `sufficientEvidence`.
- **Reviewer:** `decision`, per-hypothesis `claimReviews` with evidence IDs and support flag, `unresolvedQuestions`, `rationale`. The Reviewer inspects evidence excerpts, not only IDs.

`ACCEPT` is guarded by evidence-scoping and support checks. Model output validation cannot mathematically prove that a natural-language claim is true.

## 6. REST error paths and troubleshooting

| Condition | Expected observable behavior |
| --- | --- |
| Incident doesn't exist | 404 |
| Async executor at capacity | 503 with code `WORKFLOW_CAPACITY` |
| Workflow doesn't exist | 404 |
| Step output ID belongs to another workflow | 404 |
| Output not persisted | 404 |
| Valid workflow but incomplete investigation | Poll until terminal; inspect `failureReason` and persisted step `errorCode` |
| Network interruption | Keep workflow ID and reconnect; do not blindly resubmit |

The frontend's `ApiError` formats common 400/404/409/429/502/504/5xx cases but those status codes are **not a promise** that every listed condition is emitted by each endpoint.

## 7. PowerShell read-only inspection

```powershell
$api = "http://localhost:8080/api/v1"
$incidentId = "INC-DB-001"
$submission = Invoke-RestMethod -Method Post -Uri "$api/incidents/$incidentId/workflows/async"
$id = $submission.workflowId
Invoke-RestMethod "$api/workflows/$id"
Invoke-RestMethod "$api/workflows/$id/steps"
# After a step ID is returned:
# Invoke-RestMethod "$api/workflows/$id/steps/$stepId/output"
```

Use only synthetic incidents in this demo. Never treat `COMPLETED` as proof of a specific cause without checking the Reviewer decision and cited operational evidence.

## 8. Frontend navigation

`App.tsx` loads incidents, starts async work, stores `tracepilot.activeWorkflow.v1` in localStorage, polls workflow/steps, then fetches final role outputs and step evidence. `WorkflowMonitor.tsx` can inspect a known workflow UUID. The local `ApprovalPanel` is not a persisted API-backed approval endpoint.

Related: [architecture](TRACEPILOT_ARCHITECTURE.md), [integration](TRACEPILOT_INTEGRATION_GUIDE.md).
