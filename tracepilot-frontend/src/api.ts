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
  rootCause: unknown | null
  review: unknown | null
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
}
