import { useEffect, useState } from 'react'
import { api, type Incident, type WorkflowExecution, type WorkflowStep } from './api'

export function App() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [selected, setSelected] = useState<Incident | null>(null)
  const [workflow, setWorkflow] = useState<WorkflowExecution | null>(null)
  const [steps, setSteps] = useState<WorkflowStep[]>([])
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    api.incidents()
      .then((items) => {
        if (active) {
          setIncidents(items)
          setSelected(items[0] ?? null)
        }
      })
      .catch((err: unknown) => {
        if (active) setError(err instanceof Error ? err.message : 'Unable to load incidents')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function investigate() {
    if (!selected || running) return
    setRunning(true)
    setError(null)
    setWorkflow(null)
    setSteps([])
    try {
      // The current backend POST completes synchronously; a later phase
      // will add status polling if workflow execution becomes asynchronous.
      const result = await api.startWorkflow(selected.id)
      setWorkflow(result)
      setSteps(await api.workflowSteps(result.workflowId))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Investigation failed')
    } finally {
      setRunning(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">T</span><span>TracePilot <strong>AI</strong></span></div>
        <div className="section-label">OPERATIONS</div>
        <div className="nav-active">Incident investigation</div>
        <div className="sidebar-bottom">Milestone 5 · Dashboard alpha</div>
      </aside>
      <main className="main">
        <header className="page-header">
          <div>
            <p className="eyebrow">INCIDENT OPERATIONS</p>
            <h1>Investigation workspace</h1>
            <p className="subtitle">Investigate synthetic incidents with evidence-grounded agent workflows.</p>
          </div>
          <span className="environment">● Local environment</span>
        </header>
        {error && <div role="alert" className="error">{error}. Confirm the backend is running on port 8080.</div>}
        <div className="workspace">
          <section className="panel incident-list">
            <div className="panel-heading"><h2>Incidents</h2><span className="count">{incidents.length}</span></div>
            {loading ? <p className="muted">Loading incidents…</p> : incidents.length === 0 ? <p className="muted">No incidents found.</p> :
              <div className="incident-items">
                {incidents.map((incident) => (
                  <button key={incident.id} className={'incident-item ' + (selected?.id === incident.id ? 'selected' : '')}
                    onClick={() => { setSelected(incident); setWorkflow(null); setSteps([]); setError(null) }}>
                    <span className="incident-id">{incident.id}</span>
                    <strong>{incident.title}</strong>
                    <span className="muted">{incident.serviceName}</span>
                  </button>
                ))}
              </div>}
          </section>
          <section className="panel detail">
            {!selected ? <p className="muted">Select an incident to begin.</p> :
              <>
                <div className="detail-top"><div><p className="eyebrow">{selected.id}</p><h2>{selected.title}</h2></div><span className="pill">{selected.serviceName}</span></div>
                <p className="description">{selected.description}</p>
                <div className="metadata"><div><span>Occurred</span><strong>{new Date(selected.occurredAt).toLocaleString()}</strong></div><div><span>Service</span><strong>{selected.serviceName}</strong></div></div>
                <button className="primary" onClick={investigate} disabled={running}>{running ? 'Investigating…' : 'Start investigation →'}</button>
                {running && <p className="muted" role="status">The backend is executing the four-agent workflow. This request may take several minutes.</p>}
                {workflow && <div className="result">
                  <div className="result-heading"><h3>Workflow result</h3><span className="pill">{workflow.status.replaceAll('_', ' ')}</span></div>
                  <p className="muted">Workflow ID: <code>{workflow.workflowId}</code></p>
                  <p className="muted">Reviewer follow-ups: {workflow.followUpsUsed}</p>
                  <h3>Agent execution trace</h3>
                  <ol className="trace">{steps.map((step) => <li key={step.id}><div><strong>{step.agentRole.replaceAll('_', ' ')}</strong><span className="muted">{step.status}</span></div><small>{step.durationMs == null ? '—' : step.durationMs + ' ms'}</small></li>)}</ol>
                  {steps.length === 0 && <p className="muted">No persisted steps returned.</p>}
                </div>}
              </>}
          </section>
        </div>
      </main>
    </div>
  )
}
