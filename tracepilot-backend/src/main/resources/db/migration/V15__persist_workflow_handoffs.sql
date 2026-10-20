
ALTER TABLE workflow_steps
    ADD COLUMN investigation_run_id UUID
        REFERENCES investigation_runs(id),
    ADD COLUMN output_json JSONB,
    ADD COLUMN output_schema_version INTEGER;

ALTER TABLE workflow_steps
    ADD CONSTRAINT chk_workflow_output_schema
    CHECK (
        (output_json IS NULL
         AND output_schema_version IS NULL)
        OR
        (output_json IS NOT NULL
         AND output_schema_version >= 1)
    );

ALTER TABLE workflow_steps
    ADD CONSTRAINT chk_investigation_step_link
    CHECK (
        investigation_run_id IS NULL
        OR agent_role = 'INVESTIGATOR'
    );

CREATE UNIQUE INDEX idx_workflow_step_investigation_run
    ON workflow_steps(investigation_run_id)
    WHERE investigation_run_id IS NOT NULL;

CREATE INDEX idx_workflow_step_output
    ON workflow_steps(workflow_id, agent_role, step_number DESC)
    WHERE output_json IS NOT NULL;
