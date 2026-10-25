import { useEffect, useState } from 'react'
import { WorkflowMonitor } from './WorkflowMonitor'
import {
  api, type EvidenceReference, type Incident, type WorkflowExecution,
  type WorkflowStep, type WorkflowStepOutput,
} from './api'

type StepDetail = { output?: WorkflowStepOutput; evidence: EvidenceReference[]; error?: string }
const label = (value: string) => value.replaceAll('_', ' ')
const statusClass = (value: string) => value.toLowerCase().replaceAll('_', '-')

function StepDetails({ detail }: { detail?: StepDetail }) {
  if (!detail) return <p className="muted">No details loaded.</p>
  if (detail.error) return <p role="alert" className="muted">{detail.error}</p>
  if (!detail.output) return <p className="muted">No persisted agent output available.</p>
  let output: unknown
  try { output = JSON.parse(detail.output.json) } catch { output = detail.output.json }
  return <div className="step-detail">
    <p className="muted">Output schema v{detail.output.schemaVersion}</p>
    <pre className="output-json">{JSON.stringify(output, null, 2)}</pre>
    {detail.output.investigationRunId && <>
      <h4>Persisted evidence ({detail.evidence.length})</h4>
      {detail.evidence.length === 0
        ? <p className="muted">No evidence references returned.</p>
        : <div className="evidence-list">{detail.evidence.map(ref =>
          <div className="evidence-row" key={ref.evidenceId}>
            <span className="evidence-type">{ref.evidenceType}</span>
            <code>{ref.evidenceId}</code>
          </div>,
        )}</div>}
    </>}
  </div>
}

function Report({ workflow }: { workflow: WorkflowExecution }) {
  const { rootCause, review } = workflow
  return <section className="report" aria-label="Investigation report">
    <h3>Investigation report</h3>
    <p className={'outcome outcome-' + statusClass(workflow.status)}>
      {workflow.status === 'INSUFFICIENT_EVIDENCE'
        ? 'Evidence was insufficient to establish a verified root cause.'
        : workflow.status === 'COMPLETED'
          ? 'Workflow completed. Inspect the Reviewer decision and cited evidence.'
          : 'Workflow ended with status: ' + label(workflow.status)}
    </p>
    {rootCause ? <>
      <div className="report-heading"><h4>Root-cause hypotheses</h4>
        <span className="pill">{rootCause.sufficientEvidence ? 'Evidence sufficient' : 'Not yet proven'}</span>
      </div>
      {rootCause.hypotheses.map((hypothesis, index) =>
        <article className="hypothesis" key={index}>
          <div className="report-heading"><strong>Hypothesis {index + 1}</strong><span className="pill">{hypothesis.confidence} confidence</span></div>
          <p>{hypothesis.description}</p>
          <p className="muted">Supporting: {hypothesis.supportingEvidenceIds.join(', ') || 'None'}</p>
          <p className="muted">Contradicting: {hypothesis.contradictingEvidenceIds.join(', ') || 'None'}</p>
        </article>,
      )}
      {rootCause.openQuestions.length > 0 && <p className="muted">Open questions: {rootCause.openQuestions.join(' · ')}</p>}
    </> : <p className="muted">No root-cause analysis was returned.</p>}
    {review ? <div className="reviewer-report">
      <div className="report-heading"><h4>Reviewer decision</h4><span className="pill">{label(review.decision)}</span></div>
      <p>{review.rationale}</p>
      {review.claimReviews.map((claim, index) => <p className="review-claim" key={index}>
        <strong>{claim.supported ? 'Supported' : 'Unproven'} claim {claim.hypothesisIndex + 1}:</strong> {claim.explanation}
        <span className="muted"> Citations: {claim.evidenceIds.join(', ') || 'None'}</span>
      </p>)}
      {review.unresolvedQuestions.length > 0 &&
        <p className="muted">Unresolved: {review.unresolvedQuestions.join(' · ')}</p>}
    </div> : <p className="muted">No Reviewer decision was returned.</p>}
  </section>
}

export function App() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [selected, setSelected] = useState<Incident | null>(null)
  const [workflow, setWorkflow] = useState<WorkflowExecution | null>(null)
  const [steps, setSteps] = useState<WorkflowStep[]>([])
  const [details, setDetails] = useState<Record<string, StepDetail>>({})
  const [expanded, setExpanded] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [incidentError, setIncidentError] = useState(false)
  const [traceError, setTraceError] = useState<string | null>(null)

  async function reloadIncidents() {
    setLoading(true)
    setIncidentError(false)
    setError(null)
    try {
      const items = await api.incidents()
      setIncidents(items)
      setSelected(current => items.find(item => item.id === current?.id) ?? items[0] ?? null)
    } catch (err: unknown) {
      setIncidentError(true)
      setError(err instanceof Error ? err.message : 'Unable to load incidents')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    api.incidents()
      .then(items => { if (active) { setIncidents(items); setSelected(items[0] ?? null) } })
      .catch((err: unknown) => { if (active) { setIncidentError(true); setError(err instanceof Error ? err.message : 'Unable to load incidents') } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  function selectIncident(incident: Incident) {
    if (running) return
    setSelected(incident)
    setWorkflow(null)
    setSteps([])
    setDetails({})
    setExpanded(null)
    setError(null)
    setTraceError(null)
  }

  async function loadTrace(id: string) {
    setTraceError(null)
    try {
      const trace = await api.workflowSteps(id)
      setSteps(trace)
      const entries = await Promise.all(trace.map(async step => {
        try {
          const output = await api.stepOutputById(id, step.id)
          let evidence: EvidenceReference[] = []
          let evidenceError: string | undefined
          if (output.investigationRunId) {
            try { evidence = await api.evidence(output.investigationRunId) }
            catch { evidenceError = 'Evidence references could not be loaded.' }
          }
          return [step.id, { output, evidence, error: evidenceError }] as const
        } catch {
          return [step.id, { evidence: [], error: 'Step output unavailable.' }] as const
        }
      }))
      setDetails(Object.fromEntries(entries))
    } catch (err: unknown) {
      setTraceError(err instanceof Error ? err.message : 'Cannot load the agent trace')
    }
  }

  async function investigate() {
    if (!selected || running) return
    setRunning(true)
    setError(null)
    setWorkflow(null)
    setSteps([])
    setDetails({})
    setExpanded(null)
    setTraceError(null)
    try {
      const result = await api.startWorkflow(selected.id)
      setWorkflow(result)
      await loadTrace(result.workflowId)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Investigation failed')
    } finally {
      setRunning(false)
    }
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark">T</span><span>TracePilot <strong>AI</strong></span></div>
      <div className="section-label">OPERATIONS</div>
      <div className="nav-active">Incident investigation</div>
      <div className="sidebar-bottom">Milestone 5 · Evidence explorer</div>
    </aside>
    <main className="main">
      <header className="page-header">
        <div><p className="eyebrow">INCIDENT OPERATIONS</p>
          <h1>Investigation workspace</h1>
          <p className="subtitle">Evidence-grounded workflows with auditable agent traces.</p></div>
        <span className="environment">● Local environment</span>
      </header>
      {error && <div role="alert" className="error">
        <span>{error}</span>
        {incidentError && <button type="button" className="retry" disabled={loading} onClick={() => void reloadIncidents()}>Retry loading incidents</button>}
      </div>}
      <div className="workspace">
        <section className="panel incident-list">
          <div className="panel-heading"><h2>Incidents</h2><span className="count">{incidents.length}</span></div>
          {loading ? <p className="muted">Loading incidents…</p> : incidents.length === 0
            ? <p className="muted">No incidents found.</p>
            : <div className="incident-items">{incidents.map(incident =>
              <button type="button" key={incident.id}
                className={'incident-item ' + (selected?.id === incident.id ? 'selected' : '')}
                disabled={running} onClick={() => selectIncident(incident)}>
                <span className="incident-id">{incident.id}</span><strong>{incident.title}</strong>
                <span className="muted">{incident.serviceName}</span>
              </button>)}</div>}
        </section>
        <section className="panel detail">
          {!selected ? <p className="muted">Select an incident to begin.</p> : <>
            <div className="detail-top"><div><p className="eyebrow">{selected.id}</p><h2>{selected.title}</h2></div><span className="pill">{selected.serviceName}</span></div>
            <p className="description">{selected.description}</p>
            <div className="metadata">
              <div><span>Occurred</span><strong>{new Date(selected.occurredAt).toLocaleString()}</strong></div>
              <div><span>Service</span><strong>{selected.serviceName}</strong></div>
            </div>
            <button className="primary" type="button" onClick={investigate} disabled={running}>
              {running ? 'Investigating…' : 'Start investigation →'}
            </button>
            {running && <p className="muted" role="status">Running workflow and loading persisted step details…</p>}
            {workflow && <div className="result">
              <div className="result-heading"><h3>Workflow result</h3><span className={'pill status-' + statusClass(workflow.status)}>{label(workflow.status)}</span></div>
              <p className="muted">Workflow ID: <code>{workflow.workflowId}</code></p>
              <p className="muted">Reviewer follow-ups: {workflow.followUpsUsed}</p>
              {traceError && <div role="alert" className="error">
                <span>Agent trace unavailable: {traceError}. The workflow result is preserved.</span>
                <button type="button" className="retry" onClick={() => void loadTrace(workflow.workflowId)}>Retry loading trace</button>
              </div>}
              <Report workflow={workflow} />
              <h3>Agent execution trace</h3>
              <ol className="trace">{steps.map(step =>
                <li key={step.id} className="trace-entry">
                  <button type="button" className="trace-toggle" aria-expanded={expanded === step.id}
                    onClick={() => setExpanded(expanded === step.id ? null : step.id)}>
                    <span className="trace-identity"><strong>#{step.stepNumber} {label(step.agentRole)}</strong>
                      <span className="muted">{step.status}{step.errorCode ? ' · ' + step.errorCode : ''}</span></span>
                    <span className="trace-timing">{step.durationMs == null ? '—' : step.durationMs + ' ms'} {expanded === step.id ? '▴' : '▾'}</span>
                  </button>
                  {expanded === step.id && <div className="trace-content">
                    {step.outputSummary && <p>{step.outputSummary}</p>}
                    <StepDetails detail={details[step.id]} />
                  </div>}
                </li>)}</ol>
              {steps.length === 0 && <p className="muted">No persisted steps returned.</p>}
            </div>}
          </>}
        </section>
      </div>
      <WorkflowMonitor />
    </main>
  </div>
}
