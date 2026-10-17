
CREATE TABLE log_evidence (
    evidence_id VARCHAR(100) PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL
        REFERENCES incidents(id),
    service_name VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,

    CONSTRAINT chk_log_level
        CHECK (level IN (
            'DEBUG', 'INFO', 'WARN', 'ERROR'
        ))
);

CREATE INDEX idx_log_evidence_incident_time
    ON log_evidence (
        incident_id,
        occurred_at,
        evidence_id
    );
