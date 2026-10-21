
package com.tracepilot.workflow.triage;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowLeaseService;

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
    private final WorkflowLeaseService leases;

    public TriageWorkflowService(
            IncidentRepository incidents,
            TriageAgent triageAgent,
            WorkflowPersistenceService workflows,
            WorkflowLeaseService leases) {

        this.incidents = Objects.requireNonNull(incidents);
        this.triageAgent = Objects.requireNonNull(triageAgent);
        this.workflows = Objects.requireNonNull(workflows);
        this.leases = Objects.requireNonNull(leases);
    }

    public TriageResult execute(UUID workflowId) {

        var run = workflows.get(workflowId);
        WorkflowLease lease = leases.acquire(
                run.id(),
                UUID.randomUUID()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Workflow is already owned"));

        var incident = incidents.findById(run.incidentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown incident"));

        var step = workflows.beginStep(
                workflowId, AgentRole.TRIAGE, lease);

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

            workflows.completeStepWithOutput(
                    step.id(),
                    AgentRole.TRIAGE,
                    summary,
                    result,
                    null,
                    lease
            );

            return result;

        } catch (RuntimeException ex) {
            // The LLM failure is recorded separately
            // from the agent's execution transaction.
            workflows.failStep(step.id(), "TRIAGE_FAILED", lease);

            // For Step 3, terminate the unsuccessful run.
            // Future orchestrator handles finer error mapping.
            workflows.finishWorkflow(
                    workflowId,
                    com.tracepilot.workflow.domain.WorkflowStatus.FAILED,
                    lease
            );

            throw ex;
        }
    }
}
