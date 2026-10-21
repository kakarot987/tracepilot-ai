
ALTER TABLE workflow_runs
    ADD COLUMN execution_owner UUID,
    ADD COLUMN lease_expires_at TIMESTAMPTZ,
    ADD COLUMN fencing_token BIGINT NOT NULL DEFAULT 0;

ALTER TABLE workflow_runs
    ADD CONSTRAINT chk_workflow_lease
    CHECK (
        (execution_owner IS NULL
         AND lease_expires_at IS NULL)
        OR
        (execution_owner IS NOT NULL
         AND lease_expires_at IS NOT NULL)
    );

CREATE INDEX idx_workflow_expired_leases
    ON workflow_runs(lease_expires_at)
    WHERE execution_owner IS NOT NULL;
