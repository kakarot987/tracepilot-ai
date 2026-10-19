
package com.tracepilot.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowState(
        UUID workflowId,
        String incidentId,
        WorkflowStatus status,
        AgentRole currentRole,
        List<String> evidenceIds,
        List<Hypothesis> hypotheses,
        List<String> openQuestions,
        int stepsUsed,
        int followUpsUsed,
        Instant startedAt
) {
    public WorkflowState {
        if (workflowId == null) {
            throw new IllegalArgumentException(
                    "Workflow ID is required"
            );
        }

        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident ID is required"
            );
        }

        if (status == null || startedAt == null) {
            throw new IllegalArgumentException(
                    "Status and start time are required"
            );
        }

        if (stepsUsed < 0 || followUpsUsed < 0) {
            throw new IllegalArgumentException(
                    "Workflow counters cannot be negative"
            );
        }

        evidenceIds = List.copyOf(
                evidenceIds == null ? List.of() : evidenceIds
        );

        hypotheses = List.copyOf(
                hypotheses == null ? List.of() : hypotheses
        );

        openQuestions = List.copyOf(
                openQuestions == null ? List.of() : openQuestions
        );
    }
}
