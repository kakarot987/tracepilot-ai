
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;

import java.util.UUID;

public record WorkflowStepOutput(
        UUID stepId,
        UUID workflowId,
        AgentRole agentRole,
        int schemaVersion,
        String json,
        UUID investigationRunId
) {
    public WorkflowStepOutput {
        if (stepId == null || workflowId == null
                || agentRole == null) {
            throw new IllegalArgumentException(
                    "Step identity and role are required");
        }

        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "Invalid output schema version");
        }

        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "Persisted output cannot be blank");
        }
    }
}
