
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationStatus;

import java.util.UUID;

public record AgentExecution(
        UUID runId,
        InvestigationStatus status,
        String draftAnalysis,
        int toolCallsUsed
) {}
