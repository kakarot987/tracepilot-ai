
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStepStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkflowStepRecord(
        UUID id,
        UUID workflowId,
        int stepNumber,
        AgentRole agentRole,
        WorkflowStepStatus status,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        String outputSummary,
        String errorCode
) {}
