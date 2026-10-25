import { useState } from 'react'

type Decision = 'APPROVED' | 'REJECTED'
export function ApprovalPanel() {
  const [proposal, setProposal] = useState('')
  const [decision, setDecision] = useState<Decision | null>(null)
  return <section className="panel approval-panel" aria-label="Human approval">
    <h2>Human review of proposed actions</h2>
    <p className="muted">Review-only sandbox. Enter a proposed write action for a local decision. No actions are executed, and decisions are not sent to the backend or persisted.</p>
    <label htmlFor="action-proposal">Proposed action (manual draft)</label>
    <textarea id="action-proposal" rows={3} value={proposal} maxLength={500}
      onChange={event => { setProposal(event.target.value); setDecision(null) }}
      placeholder="Example: Increase the service connection pool limit after reviewing impact" />
    <div className="approval-controls">
      <button type="button" disabled={!proposal.trim()} onClick={() => setDecision('APPROVED')}>Approve for review</button>
      <button type="button" disabled={!proposal.trim()} onClick={() => setDecision('REJECTED')}>Reject proposal</button>
    </div>
    {decision && <p role="status" className="muted">Local review decision: {decision}. No remediation has been executed.</p>}
  </section>
}
