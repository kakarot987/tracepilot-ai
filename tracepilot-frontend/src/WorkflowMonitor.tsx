import { useEffect, useState, type FormEvent } from 'react'
import { api, type WorkflowRun, type WorkflowStep } from './api'

const isTerminal = (status: string) =>
  ['COMPLETED', 'INSUFFICIENT_EVIDENCE', 'FAILED', 'TIMED_OUT', 'CANCELLED'].includes(status)

export function WorkflowMonitor() {
  const [input, setInput] = useState('')
  const [id, setId] = useState('')
  const [run, setRun] = useState<WorkflowRun | null>(null)
  const [steps, setSteps] = useState<WorkflowStep[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let active = true
    let timer: ReturnType<typeof setTimeout> | undefined
    async function refresh() {
      try {
        const [state, trace] = await Promise.all([api.workflow(id), api.workflowSteps(id)])
        if (!active) return
        setRun(state)
        setSteps(trace)
        setError(null)
        if (!isTerminal(state.status)) timer = setTimeout(() => void refresh(), 2500)
      } catch (err: unknown) {
        if (!active) return
        setError(err instanceof Error ? err.message : 'Unable to load workflow')
        // Stop automatic retries on errors; user can inspect again.
      }
    }
    void refresh()
    return () => { active = false; if (timer) clearTimeout(timer) }
  }, [id])

  function inspect(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = input.trim()
    if (!/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(next)) {
      setError('Enter a valid workflow UUID')
      return
    }
    setRun(null)
    setSteps([])
    setError(null)
    setId(next)
  }

  return <section className="panel monitor" aria-label="Workflow monitor">
    <h2>Workflow monitor</h2>
    <p className="muted">Inspect a persisted workflow by ID. Active workflows refresh automatically every 2.5 seconds.</p>
    <form className="monitor-form" onSubmit={inspect}>
      <input aria-label="Workflow ID" placeholder="Paste workflow UUID" value={input} onChange={event => setInput(event.target.value)} />
      <button type="submit" className="primary">Inspect</button>
    </form>
    {error && <p role="alert" className="monitor-error">{error}</p>}
    {run && <div className="monitor-summary">
      <div className="report-heading"><strong>{run.status.replaceAll('_', ' ')}</strong><span className="muted">{isTerminal(run.status) ? 'Final state' : 'Polling'}</span></div>
      <p className="muted">Incident: {run.incidentId} · Steps: {run.stepsUsed} · Follow-ups: {run.followUpsUsed}</p>
      {run.currentRole && <p>Active agent: <strong>{run.currentRole.replaceAll('_', ' ')}</strong></p>}
      {run.failureReason && <p role="alert" className="monitor-error">{run.failureReason}</p>}
      <ol className="monitor-steps">{steps.map(step =>
        <li key={step.id}>#{step.stepNumber} {step.agentRole.replaceAll('_', ' ')} — {step.status}</li>)}</ol>
    </div>}
  </section>
}
