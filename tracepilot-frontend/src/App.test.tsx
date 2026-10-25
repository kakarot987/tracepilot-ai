import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { App } from './App'
import { api, type Incident, type WorkflowExecution, type WorkflowStep } from './api'

vi.mock('./api', () => ({
  api: {
    incidents: vi.fn(),
    workflow: vi.fn(),
    startWorkflow: vi.fn(),
    startAsyncWorkflow: vi.fn(),
    stepOutput: vi.fn(),
    workflowSteps: vi.fn(),
    stepOutputById: vi.fn(),
    evidence: vi.fn(),
  },
}))

const incidents: Incident[] = [
  { id: 'INC-DB-001', title: 'Database connection failures', description: 'Timeouts', serviceName: 'order-service', occurredAt: '2026-10-01T10:00:00Z' },
  { id: 'INC-PAY-001', title: 'Payment request timeouts', description: 'Gateway timeout', serviceName: 'payment-service', occurredAt: '2026-10-01T10:00:00Z' },
]
const result: WorkflowExecution = {
  workflowId: 'workflow-1', status: 'INSUFFICIENT_EVIDENCE', followUpsUsed: 1,
  rootCause: { sufficientEvidence: false, openQuestions: ['What caused this?'], hypotheses: [
    { description: 'Connection pool acquisition failed', confidence: 'MEDIUM', supportingEvidenceIds: ['LOG-DB-001'], contradictingEvidenceIds: [] },
  ] },
  review: { decision: 'FOLLOW_UP', rationale: 'Cause unproven', unresolvedQuestions: ['What changed?'], claimReviews: [
    { hypothesisIndex: 0, supported: false, evidenceIds: ['LOG-DB-001'], explanation: 'Not enough evidence' },
  ] },
}
const steps: WorkflowStep[] = [
  { id: 'step-1', workflowId: 'workflow-1', stepNumber: 1, agentRole: 'INVESTIGATOR', status: 'COMPLETED', durationMs: 200, errorCode: null, outputSummary: 'Evidence collected' },
]

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.incidents).mockResolvedValue(incidents)
  vi.mocked(api.startWorkflow).mockResolvedValue(result)
  vi.mocked(api.startAsyncWorkflow).mockResolvedValue({ workflowId: 'workflow-1', status: 'PENDING' })
  vi.mocked(api.workflow).mockResolvedValue({ id: 'workflow-1', incidentId: 'INC-DB-001', status: 'INSUFFICIENT_EVIDENCE', stepsUsed: 1, followUpsUsed: 1, currentRole: null, startedAt: '2026-10-01T10:00:00Z', completedAt: '2026-10-01T10:01:00Z', failureReason: null })
  vi.mocked(api.stepOutput).mockImplementation(async (_id, role) => ({ stepId: 'step-1', workflowId: 'workflow-1', agentRole: role, schemaVersion: 1, investigationRunId: null, json: JSON.stringify(role === 'ROOT_CAUSE' ? result.rootCause : result.review) }))
  vi.mocked(api.workflowSteps).mockResolvedValue(steps)
  vi.mocked(api.stepOutputById).mockResolvedValue({
    stepId: 'step-1', workflowId: 'workflow-1', agentRole: 'INVESTIGATOR',
    investigationRunId: 'run-1', schemaVersion: 1, json: '{"status":"COMPLETED"}',
  })
  vi.mocked(api.evidence).mockResolvedValue([
    { investigationId: 'run-1', evidenceId: 'LOG-DB-001', evidenceType: 'LOG', recordedAt: '2026-10-01T10:00:00Z' },
  ])
})

describe('Investigation workspace', () => {
  it('loads incidents and switches the selected incident', async () => {
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Database connection failures' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /INC-PAY-001/i }))
    expect(screen.getByRole('heading', { name: 'Payment request timeouts' })).toBeInTheDocument()
  })

  it('renders insufficient evidence, reviewer rationale and step evidence', async () => {
    render(<App />)
    await screen.findByRole('heading', { name: 'Database connection failures' })
    fireEvent.click(screen.getByRole('button', { name: /Start investigation/i }))
    expect(await screen.findByText('Cause unproven')).toBeInTheDocument()
    expect(screen.getByText(/Evidence was insufficient/)).toBeInTheDocument()
    const toggle = await screen.findByRole('button', { name: /#1 INVESTIGATOR/i })
    fireEvent.click(toggle)
    await waitFor(() => expect(within(screen.getByText(/Persisted evidence \(1\)/).parentElement!).getByText('LOG-DB-001')).toBeInTheDocument())
    expect(api.evidence).toHaveBeenCalledWith('run-1')
  })

  it('shows API error when incident loading fails', async () => {
    vi.mocked(api.incidents).mockRejectedValueOnce(new Error('Unable to reach API'))
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach API')
  })

  it('keeps incident visible when workflow execution fails', async () => {
    vi.mocked(api.startAsyncWorkflow).mockRejectedValueOnce(new Error('Gateway timeout'))
    render(<App />)
    await screen.findByRole('heading', { name: 'Database connection failures' })
    fireEvent.click(screen.getByRole('button', { name: /Start investigation/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Gateway timeout')
    expect(screen.getByRole('heading', { name: 'Database connection failures' })).toBeInTheDocument()
    expect(screen.queryByText('Workflow result')).not.toBeInTheDocument()
  })
  it('retries incident loading after a transient error', async () => {
    vi.mocked(api.incidents).mockRejectedValueOnce(new Error('Network unavailable'))
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Network unavailable')
    fireEvent.click(screen.getByRole('button', { name: 'Retry loading incidents' }))
    expect(await screen.findByRole('heading', { name: 'Database connection failures' })).toBeInTheDocument()
    expect(api.incidents).toHaveBeenCalledTimes(2)
  })

  it('preserves workflow report and retries a failed trace request without rerunning', async () => {
    vi.mocked(api.workflowSteps).mockRejectedValueOnce(new Error('Backend not ready'))
    render(<App />)
    await screen.findByRole('heading', { name: 'Database connection failures' })
    fireEvent.click(screen.getByRole('button', { name: /Start investigation/i }))
    expect(await screen.findByText('Cause unproven')).toBeInTheDocument()
    expect(await screen.findByText(/Agent trace unavailable/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry loading trace' }))
    expect(await screen.findByRole('button', { name: /#1 INVESTIGATOR/i })).toBeInTheDocument()
    expect(api.startAsyncWorkflow).toHaveBeenCalledTimes(1)
    expect(api.workflowSteps).toHaveBeenCalledTimes(2)
  })

})
