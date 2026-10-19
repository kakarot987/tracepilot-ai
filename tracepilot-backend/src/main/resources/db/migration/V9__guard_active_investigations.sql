CREATE UNIQUE INDEX uq_active_investigation_per_incident
    ON investigation_runs (incident_id)
    WHERE status = 'RUNNING';