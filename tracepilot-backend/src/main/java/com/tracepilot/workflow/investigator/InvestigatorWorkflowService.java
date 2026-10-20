
package com.tracepilot.workflow.investigator;

import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.workflow.orchestration.FollowUpFocus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class InvestigatorWorkflowService {

    private final InvestigationAgent investigator;
    private final InvestigationService investigations;
    private final WorkflowPersistenceService workflows;

    public InvestigatorWorkflowService(
            InvestigationAgent investigator,
            InvestigationService investigations,
            WorkflowPersistenceService workflows) {

        this.investigator = Objects.requireNonNull(investigator);
        this.investigations = Objects.requireNonNull(investigations);
        this.workflows = Objects.requireNonNull(workflows);
    }

    public InvestigatorStepResult execute(UUID workflowId) {
        return execute(workflowId, null);
    }

    public InvestigatorStepResult execute(
            UUID workflowId,
            FollowUpFocus focus) {

        var workflow = workflows.get(workflowId);

        var step = workflows.beginStep(
                workflowId, AgentRole.INVESTIGATOR);

        try {
            var execution = focus == null
                    ? investigator.investigate(workflow.incidentId())
                    : investigator.investigate(workflow.incidentId(), focus);

            var evidenceIds = investigations
                    .evidence(execution.runId())
                    .stream()
                    .map(reference -> reference.evidenceId())
                    .distinct()
                    .toList();

            InvestigatorStepResult result =
                    new InvestigatorStepResult(
                            execution.runId(),
                            execution.status(),
                            execution.draftAnalysis(),
                            evidenceIds,
                            execution.toolCallsUsed()
                    );

            switch (result.status()) {

                case COMPLETED -> {
                    String summary = """
        Investigation completed; runId=%s; evidenceCount=%d; toolCalls=%d
        """.formatted(
                            result.investigationRunId(),
                            result.evidenceIds().size(),
                            result.toolCallsUsed()
                    ).trim();

                    workflows.completeStep(step.id(), summary);
                }

                case INSUFFICIENT_EVIDENCE -> {
                    workflows.completeStep(
                            step.id(),
                            "Insufficient evidence; runId=%s"
                                    .formatted(
                                            result.investigationRunId())
                    );
                    // Leave the workflow RUNNING.
                    // Orchestrator decides the next transition.
                }

                case TIMED_OUT -> {
                    workflows.failStep(
                            step.id(), "INVESTIGATION_TIMEOUT");
                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.TIMED_OUT);
                }

                case FAILED -> {
                    workflows.failStep(
                            step.id(), "INVESTIGATION_FAILED");
                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.FAILED);
                }

                default -> throw new IllegalStateException(
                        "Unexpected investigation status");
            }

            return result;

        } catch (RuntimeException ex) {
            // A failed workflow persistence operation can
            // itself throw. Do not mask the original exception
            // or claim the workflow was finalized.
            try {
                var current = workflows.getStep(step.id());

                if (current.status() ==
                        com.tracepilot.workflow.domain
                                .WorkflowStepStatus.RUNNING) {

                    workflows.failStep(
                            step.id(), "INVESTIGATOR_STEP_FAILED");

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
