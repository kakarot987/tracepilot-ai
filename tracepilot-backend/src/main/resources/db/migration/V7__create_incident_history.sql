CREATE TABLE incident_history (
    evidence_id VARCHAR(100) PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL UNIQUE,
    service_name VARCHAR(100) NOT NULL
        REFERENCES service_info(service_name),
    occurred_at TIMESTAMPTZ NOT NULL,
    title VARCHAR(150) NOT NULL,
    summary TEXT NOT NULL
);

CREATE INDEX idx_history_service_time
    ON incident_history (
        service_name,
        occurred_at DESC,
        evidence_id
    );