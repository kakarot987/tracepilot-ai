
CREATE TABLE incidents (
    id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_incidents_service_time
    ON incidents (service_name, occurred_at DESC);

CREATE TABLE investigation_runs (
    id UUID PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL
        REFERENCES incidents(id),
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_reason TEXT,
    CONSTRAINT chk_investigation_status
        CHECK (status IN (
            'RUNNING',
            'COMPLETED',
            'FAILED',
            'TIMED_OUT',
            'INSUFFICIENT_EVIDENCE'
        )),
    CONSTRAINT chk_investigation_completed
        CHECK (
            completed_at IS NULL
            OR completed_at >= started_at
        )
);

CREATE INDEX idx_investigation_runs_incident
    ON investigation_runs (incident_id, started_at DESC);

CREATE TABLE investigation_evidence (
    investigation_id UUID NOT NULL
        REFERENCES investigation_runs(id)
        ON DELETE CASCADE,
    evidence_id VARCHAR(100) NOT NULL,
    evidence_type VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (investigation_id, evidence_id),
    CONSTRAINT chk_evidence_type
        CHECK (evidence_type IN (
            'LOG',
            'DEPLOYMENT',
            'SERVICE_INFO',
            'INCIDENT_HISTORY'
        ))
);
