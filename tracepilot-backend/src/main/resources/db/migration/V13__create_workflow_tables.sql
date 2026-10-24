
CREATE TABLE workflow_runs (
    id UUID PRIMARY KEY,
    incident_id VARCHAR(120) NOT NULL
        REFERENCES incidents(id),
    status VARCHAR(40) NOT NULL
        CHECK (status IN (
            'PENDING', 'RUNNING', 'COMPLETED',
            'INSUFFICIENT_EVIDENCE', 'FAILED',
            'TIMED_OUT', 'CANCELLED'
        )),
    active_agent_role VARCHAR(30),
    steps_used INTEGER NOT NULL DEFAULT 0
        CHECK (steps_used >= 0),
    follow_ups_used INTEGER NOT NULL DEFAULT 0
        CHECK (follow_ups_used >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL
        DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    CHECK (
        active_agent_role IS NULL OR
        active_agent_role IN (
            'TRIAGE',
            'INVESTIGATOR',
            'ROOT_CAUSE',
            'REVIEWER'
        )
    )
);

CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL
        REFERENCES workflow_runs(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL CHECK (step_number > 0),
    agent_role VARCHAR(30) NOT NULL
        CHECK (agent_role IN (
            'TRIAGE', 'INVESTIGATOR',
            'ROOT_CAUSE', 'REVIEWER'
        )),
    status VARCHAR(30) NOT NULL
        CHECK (status IN (
            'PENDING', 'RUNNING', 'COMPLETED',
            'FAILED', 'SKIPPED', 'TIMED_OUT'
        )),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT CHECK (duration_ms >= 0),
    output_summary VARCHAR(2000),
    error_code VARCHAR(100),
    UNIQUE (workflow_id, step_number)
);

CREATE INDEX idx_workflow_runs_incident
    ON workflow_runs(incident_id, started_at DESC);

CREATE INDEX idx_workflow_runs_status
    ON workflow_runs(status);

CREATE INDEX idx_workflow_steps_trace
    ON workflow_steps(workflow_id, step_number);
