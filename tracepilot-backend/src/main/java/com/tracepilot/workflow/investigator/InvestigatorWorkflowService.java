
package com.tracepilot.workflow.investigator;

import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.workflow.orchestration.FollowUpFocus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowLeaseService;
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
    private final WorkflowLeaseService leases;

    public InvestigatorWorkflowService(
            InvestigationAgent investigator,
            InvestigationService investigations,
            WorkflowPersistenceService workflows,
            WorkflowLeaseService leases) {

        this.investigator = Objects.requireNonNull(investigator);
        this.investigations = Objects.requireNonNull(investigations);
        this.workflows = Objects.requireNonNull(workflows);
        this.leases = Objects.requireNonNull(leases);
    }

    public InvestigatorStepResult execute(UUID workflowId) {
        return execute(workflowId, null);
    }

    public InvestigatorStepResult execute(
            UUID workflowId,
            FollowUpFocus focus) {

        var workflow = workflows.get(workflowId);

        WorkflowLease lease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Workflow is already owned"));

        var step = workflows.beginStep(
                workflowId, AgentRole.INVESTIGATOR,lease);



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

                    workflows.completeStepWithOutput(
                            step.id(),
                            AgentRole.INVESTIGATOR,
                            summary,
                            result,
                            result.investigationRunId(),
                            lease
                    );
                }

                case INSUFFICIENT_EVIDENCE -> {
                    workflows.completeStepWithOutput(
                            step.id(),
                            AgentRole.INVESTIGATOR,
                            "Insufficient evidence; runId=%s"
                                    .formatted(result.investigationRunId()),
                            result,
                            result.investigationRunId(),
                            lease
                    );
                    // Leave the workflow RUNNING.
                    // Orchestrator decides the next transition.
                }

                case TIMED_OUT -> {
                    workflows.failStep(
                            step.id(), "INVESTIGATION_TIMEOUT", lease);
                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.TIMED_OUT, lease);
                }

                case FAILED -> {
                    workflows.failStep(
                            step.id(), "INVESTIGATION_FAILED", lease);
                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.FAILED, lease);
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
                            step.id(), "INVESTIGATOR_STEP_FAILED", lease);

                    workflows.finishWorkflow(
                            workflowId, WorkflowStatus.FAILED, lease);
                }
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }

            throw ex;
        }
    }
}
