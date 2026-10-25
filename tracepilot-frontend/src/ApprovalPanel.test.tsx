import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { ApprovalPanel } from './ApprovalPanel'

describe('Human approval sandbox', () => {
  it('requires a proposal and records a local-only decision', () => {
    render(<ApprovalPanel />)
    const approve = screen.getByRole('button', { name: 'Approve for review' })
    expect(approve).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Proposed action (manual draft)'), { target: { value: 'Increase pool size' } })
    expect(approve).toBeEnabled()
    fireEvent.click(approve)
    expect(screen.getByRole('status')).toHaveTextContent('No remediation has been executed')
    expect(screen.getByRole('status')).toHaveTextContent('APPROVED')
  })
})
