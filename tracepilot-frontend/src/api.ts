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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch('/api/v1' + path, {
    ...init,
    headers: { Accept: 'application/json', ...init?.headers },
  })

  if (!response.ok) {
    throw new Error('API request failed (' + response.status + ')')
  }
  return response.json() as Promise<T>
}

export const api = {
  incidents: () => request<Incident[]>('/incidents'),
  startWorkflow: (id: string) =>
    request<WorkflowExecution>('/incidents/' + encodeURIComponent(id) + '/workflows', {
      method: 'POST',
    }),
  workflowSteps: (id: string) =>
    request<WorkflowStep[]>('/workflows/' + encodeURIComponent(id) + '/steps'),
  stepOutputById: (workflowId: string, stepId: string) =>
    request<WorkflowStepOutput>('/workflows/' + encodeURIComponent(workflowId) + '/steps/' + encodeURIComponent(stepId) + '/output'),
  stepOutput: (workflowId: string, role: WorkflowStep['agentRole']) =>
    request<WorkflowStepOutput>('/workflows/' + encodeURIComponent(workflowId) + '/outputs/' + role),
  evidence: (investigationId: string) =>
    request<EvidenceReference[]>('/investigations/' + encodeURIComponent(investigationId) + '/evidence'),
}
