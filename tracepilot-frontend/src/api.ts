export interface Incident {
  id: string
  title: string
  description: string
  serviceName: string
  occurredAt: string
}

export type WorkflowStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'INSUFFICIENT_EVIDENCE'
  | 'FAILED'
  | 'TIMED_OUT'
  | 'CANCELLED'

export interface WorkflowRun {
  id: string
  incidentId: string
  status: WorkflowStatus
  stepsUsed: number
  followUpsUsed: number
  currentRole: string | null
  startedAt: string
  completedAt: string | null
  failureReason: string | null
}

export interface WorkflowExecution {
  workflowId: string
  status: WorkflowStatus
  followUpsUsed: number
  rootCause: RootCauseResult | null
  review: ReviewerResult | null
}

export interface Hypothesis {
  description: string
  supportingEvidenceIds: string[]
  contradictingEvidenceIds: string[]
  confidence: 'LOW' | 'MEDIUM' | 'HIGH' | 'UNKNOWN'
}
export interface RootCauseResult {
  hypotheses: Hypothesis[]
  openQuestions: string[]
  sufficientEvidence: boolean
}
export interface ClaimReview {
  hypothesisIndex: number
  supported: boolean
  evidenceIds: string[]
  explanation: string
}
export interface ReviewerResult {
  decision: 'ACCEPT' | 'FOLLOW_UP' | 'REJECT' | 'INSUFFICIENT_EVIDENCE'
  claimReviews: ClaimReview[]
  unresolvedQuestions: string[]
  rationale: string
}
export interface WorkflowStepOutput {
  stepId: string
  workflowId: string
  agentRole: WorkflowStep['agentRole']
  schemaVersion: number
  json: string
  investigationRunId: string | null
}
export interface EvidenceReference {
  investigationId: string
  evidenceId: string
  evidenceType: string
  recordedAt: string
}
export interface WorkflowStep {
  id: string
  workflowId: string
  stepNumber: number
  agentRole: 'TRIAGE' | 'INVESTIGATOR' | 'ROOT_CAUSE' | 'REVIEWER'
  status: string
  durationMs: number | null
  errorCode: string | null
  outputSummary: string | null
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | null,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

function statusMessage(status: number): string {
  if (status === 400) return 'Request could not be processed'
  if (status === 404) return 'Requested resource was not found'
  if (status === 409) return 'Another investigation may already be active'
  if (status === 429) return 'Too many requests; try again later'
  if (status === 502) return 'Agent returned an invalid response'
  if (status === 504) return 'Investigation timed out'
  if (status >= 500) return 'Backend service is unavailable'
  return 'API request failed'
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch('/api/v1' + path, {
      ...init,
      headers: { Accept: 'application/json', ...init?.headers },
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the backend. Check that it is running.')
  }

  if (!response.ok) {
    let code: string | null = null
    try {
      const body: unknown = await response.json()
      if (body && typeof body === 'object' && 'error' in body &&
          typeof body.error === 'string') code = body.error
    } catch { /* The backend may return an empty or non-JSON error body. */ }
    throw new ApiError(response.status, code,
      statusMessage(response.status) + ' (HTTP ' + response.status + ')')
  }
  return response.json() as Promise<T>
}

export const api = {
  incidents: () => request<Incident[]>('/incidents'),
  startWorkflow: (id: string) =>
    request<WorkflowExecution>('/incidents/' + encodeURIComponent(id) + '/workflows', {
      method: 'POST',
    }),
  workflow: (id: string) =>
    request<WorkflowRun>('/workflows/' + encodeURIComponent(id)),
  workflowSteps: (id: string) =>
    request<WorkflowStep[]>('/workflows/' + encodeURIComponent(id) + '/steps'),
  stepOutputById: (workflowId: string, stepId: string) =>
    request<WorkflowStepOutput>('/workflows/' + encodeURIComponent(workflowId) + '/steps/' + encodeURIComponent(stepId) + '/output'),
  stepOutput: (workflowId: string, role: WorkflowStep['agentRole']) =>
    request<WorkflowStepOutput>('/workflows/' + encodeURIComponent(workflowId) + '/outputs/' + role),
  evidence: (investigationId: string) =>
    request<EvidenceReference[]>('/investigations/' + encodeURIComponent(investigationId) + '/evidence'),
}
