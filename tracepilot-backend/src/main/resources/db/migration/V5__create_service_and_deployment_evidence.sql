
CREATE TABLE service_info (
    service_name VARCHAR(100) PRIMARY KEY,
    team VARCHAR(100) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    runtime VARCHAR(150) NOT NULL,
    database_dependency VARCHAR(100),
    upstream_dependency VARCHAR(100)
);

CREATE TABLE deployment_evidence (
    evidence_id VARCHAR(100) PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL
        REFERENCES service_info(service_name),
    version VARCHAR(50) NOT NULL,
    deployed_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    change_summary TEXT NOT NULL
);

CREATE INDEX idx_deployment_service_time
    ON deployment_evidence (
        service_name,
        deployed_at DESC,
        evidence_id
    );
