
package com.tracepilot.workflow.reviewer;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.rootcause.RootCauseResult;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class ReviewerWorkflowService {

    private final ReviewerAgent reviewer;
    private final WorkflowPersistenceService workflows;

    public ReviewerWorkflowService(
            ReviewerAgent reviewer,
            WorkflowPersistenceService workflows) {
        this.reviewer = Objects.requireNonNull(reviewer);
        this.workflows = Objects.requireNonNull(workflows);
    }

    public ReviewerResult execute(
            UUID workflowId,
            RootCauseResult rootCause,
            List<EvidenceExcerpt> evidence) {

        var workflow = workflows.get(workflowId);

        if (workflow.status() != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Workflow must be running");
        }

        var step = workflows.beginStep(
                workflowId, AgentRole.REVIEWER);

        try {
            ReviewerResult result =
                    reviewer.review(rootCause, evidence);

            String summary = (
                    "Decision=%s; Claims=%d; Questions=%d"
            ).formatted(
                    result.decision(),
                    result.claimReviews().size(),
                    result.unresolvedQuestions().size()
            );

            workflows.completeStepWithOutput(
                    step.id(),
                    AgentRole.REVIEWER,
                    summary,
                    result,
                    null
            );

            // Do not finalize here.
            // Step 7 orchestrator owns the final transition.
            return result;

        } catch (RuntimeException ex) {
            try {
                var current = workflows.getStep(step.id());

                if (current.status() == WorkflowStepStatus.RUNNING) {
                    workflows.failStep(
                            step.id(), "REVIEWER_FAILED");

                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.FAILED);
                }
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }

            throw ex;
        }
    }
}
