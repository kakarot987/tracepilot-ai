
package com.tracepilot.workflow.rootcause;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.investigator.InvestigatorStepResult;
import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class RootCauseWorkflowService {

    private final RootCauseAgent agent;
    private final WorkflowPersistenceService workflows;

    public RootCauseWorkflowService(
            RootCauseAgent agent,
            WorkflowPersistenceService workflows) {
        this.agent = Objects.requireNonNull(agent);
        this.workflows = Objects.requireNonNull(workflows);
    }

    public RootCauseResult execute(
            UUID workflowId,
            InvestigatorStepResult investigation,
            WorkflowLease lease) {

        Objects.requireNonNull(investigation);

        var workflow = workflows.get(workflowId);

        if (workflow.status() != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Workflow must be running");
        }

        // Step 7 will additionally verify that the child
        // investigation belongs to this workflow/incident.
        var step = workflows.beginStep(
                workflowId, AgentRole.ROOT_CAUSE,lease);

        try {
            RootCauseResult result = agent.analyze(investigation);

            String summary = (
                    "Hypotheses=%d; sufficientEvidence=%s; "
                            + "openQuestions=%d"
            ).formatted(
                    result.hypotheses().size(),
                    result.sufficientEvidence(),
                    result.openQuestions().size()
            );

            workflows.completeStepWithOutput(
                    step.id(),
                    AgentRole.ROOT_CAUSE,
                    summary,
                    result,
                    null,
                    lease
            );
            return result;

        } catch (RuntimeException ex) {
            try {
                var current = workflows.getStep(step.id());

                if (current.status() == WorkflowStepStatus.RUNNING) {
                    workflows.failStep(
                            step.id(), "ROOT_CAUSE_FAILED", lease);

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
