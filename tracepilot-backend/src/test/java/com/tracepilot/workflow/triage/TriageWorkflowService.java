
package com.tracepilot.workflow.triage;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class TriageWorkflowService {

    private final IncidentRepository incidents;
    private final TriageAgent triageAgent;
    private final WorkflowPersistenceService workflows;

    public TriageWorkflowService(
            IncidentRepository incidents,
            TriageAgent triageAgent,
            WorkflowPersistenceService workflows) {

        this.incidents = Objects.requireNonNull(incidents);
        this.triageAgent = Objects.requireNonNull(triageAgent);
        this.workflows = Objects.requireNonNull(workflows);
    }

    public TriageResult execute(UUID workflowId) {

        var run = workflows.get(workflowId);

        var incident = incidents.findById(run.incidentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown incident"));

        var step = workflows.beginStep(
                workflowId, AgentRole.TRIAGE);

        try {
            TriageResult result = triageAgent.triage(incident);

            // Persist a bounded, non-sensitive summary.
            String summary =
                    "Severity=%s; Category=%s; Focus=%s"
                            .formatted(
                                    result.severity(),
                                    result.category(),
                                    result.investigationFocus()
                            );

            workflows.completeStep(step.id(), summary);

            return result;

        } catch (RuntimeException ex) {
            // The LLM failure is recorded separately
            // from the agent's execution transaction.
            workflows.failStep(step.id(), "TRIAGE_FAILED");

            // For Step 3, terminate the unsuccessful run.
            // Future orchestrator handles finer error mapping.
            workflows.finishWorkflow(
                    workflowId,
                    com.tracepilot.workflow.domain.WorkflowStatus.FAILED
            );

            throw ex;
        }
    }
}
