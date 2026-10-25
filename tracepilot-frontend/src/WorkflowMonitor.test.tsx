import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { WorkflowMonitor } from './WorkflowMonitor'
import { api } from './api'

vi.mock('./api', () => ({
  api: { workflow: vi.fn(), workflowSteps: vi.fn() },
}))
const id = '02b5f9ee-8c34-4ae7-bb49-4a501b49fa16'

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.workflow).mockResolvedValue({
    id, incidentId: 'INC-PAY-001', status: 'INSUFFICIENT_EVIDENCE',
    stepsUsed: 7, followUpsUsed: 1, currentRole: null,
    startedAt: '2026-10-01T10:00:00Z', completedAt: '2026-10-01T10:01:00Z',
    failureReason: null,
  })
  vi.mocked(api.workflowSteps).mockResolvedValue([])
})

describe('Workflow monitor', () => {
  it('rejects malformed workflow identifiers without an API call', () => {
    render(<WorkflowMonitor />)
    fireEvent.change(screen.getByRole('textbox', { name: 'Workflow ID' }), { target: { value: 'bad-id' } })
    fireEvent.click(screen.getByRole('button', { name: 'Inspect' }))
    expect(screen.getByRole('alert')).toHaveTextContent('valid workflow UUID')
    expect(api.workflow).not.toHaveBeenCalled()
  })

  it('retrieves persisted status without starting an investigation', async () => {
    render(<WorkflowMonitor />)
    fireEvent.change(screen.getByRole('textbox', { name: 'Workflow ID' }), { target: { value: id } })
    fireEvent.click(screen.getByRole('button', { name: 'Inspect' }))
    expect(await screen.findByText('Final state')).toBeInTheDocument()
    expect(screen.getByText(/INC-PAY-001/)).toBeInTheDocument()
    expect(api.workflow).toHaveBeenCalledWith(id)
    expect(api.workflowSteps).toHaveBeenCalledWith(id)
  })
})
