
package com.tracepilot.workflow.investigator;

import com.tracepilot.investigation.domain.InvestigationStatus;
import java.util.List;
import java.util.UUID;

public record InvestigatorStepResult(
        UUID investigationRunId,
        InvestigationStatus status,
        String draftAnalysis,
        List<String> evidenceIds,
        int toolCallsUsed
) {
    public InvestigatorStepResult {
        evidenceIds = List.copyOf(evidenceIds);
        draftAnalysis = draftAnalysis == null ? "" : draftAnalysis;
    }
}
