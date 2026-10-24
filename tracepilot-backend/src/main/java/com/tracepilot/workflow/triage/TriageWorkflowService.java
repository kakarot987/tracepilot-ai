
package com.tracepilot.workflow.triage;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.persistence.WorkflowLease;
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
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(
                    TriageWorkflowService.class
            );
    public TriageWorkflowService(
            IncidentRepository incidents,
            TriageAgent triageAgent,
            WorkflowPersistenceService workflows) {

        this.incidents = Objects.requireNonNull(incidents);
        this.triageAgent = Objects.requireNonNull(triageAgent);
        this.workflows = Objects.requireNonNull(workflows);
    }

    public TriageResult execute(UUID workflowId, WorkflowLease lease) {

        var run = workflows.get(workflowId);

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
            log.error(
                    "Triage failed; workflowId={}, exceptionType={}",
                    workflowId,
                    ex.getClass().getName(),
                    ex
            );
            boolean timedOut =
                    ex instanceof
                            com.tracepilot.investigation.domain
                                    .InvestigationLimitExceededException
                            || "Triage deadline exceeded".equals(
                            ex.getMessage());

            String errorCode = timedOut
                    ? "TRIAGE_TIMEOUT"
                    : "TRIAGE_FAILED";

            var terminalStatus = timedOut
                    ? com.tracepilot.workflow.domain
                    .WorkflowStatus.TIMED_OUT
                    : com.tracepilot.workflow.domain
                    .WorkflowStatus.FAILED;

            try {
                var current = workflows.getStep(step.id());

                if (current.status()
                        == com.tracepilot.workflow.domain
                        .WorkflowStepStatus.RUNNING) {

                    workflows.failStep(
                            step.id(),
                            errorCode,
                            lease
                    );

                    workflows.finishWorkflow(
                            workflowId,
                            terminalStatus,
                            lease
                    );
                }
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }

            throw ex;
        }
    }
}
