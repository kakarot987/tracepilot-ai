
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkflowRunRecord(
        UUID id,
        String incidentId,
        WorkflowStatus status,
        AgentRole currentRole,
        int stepsUsed,
        int followUpsUsed,
        long version,
        Instant startedAt,
        Instant completedAt,
        String failureReason
) {}
