
package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.rootcause.RootCauseResult;
import com.tracepilot.workflow.reviewer.ReviewerResult;

import java.util.UUID;

public record WorkflowExecutionResult(
        UUID workflowId,
        WorkflowStatus status,
        int followUpsUsed,
        RootCauseResult rootCause,
        ReviewerResult review
) {}
