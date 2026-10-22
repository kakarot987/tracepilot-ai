
package com.tracepilot.workflow.api;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.orchestration.WorkflowExecutionResult;
import com.tracepilot.workflow.orchestration.WorkflowOrchestrator;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowRunRecord;
import com.tracepilot.workflow.persistence.WorkflowStepOutput;
import com.tracepilot.workflow.persistence.WorkflowStepRecord;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1")
public class WorkflowController {

    private final WorkflowOrchestrator orchestrator;
    private final WorkflowPersistenceService workflows;
    private final IncidentRepository incidents;

    public WorkflowController(
            WorkflowOrchestrator orchestrator,
            WorkflowPersistenceService workflows,
            IncidentRepository incidents) {

        this.orchestrator = orchestrator;
        this.workflows = workflows;
        this.incidents = incidents;
    }

    @PostMapping("/incidents/{incidentId}/workflows")
    public ResponseEntity<WorkflowExecutionResult> start(
            @PathVariable String incidentId) {

        // Reject an unknown incident before starting
        // an expensive orchestration run.
        if (incidents.findById(incidentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WorkflowExecutionResult result =
                orchestrator.execute(incidentId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<WorkflowRunRecord> get(
            @PathVariable UUID workflowId) {

        return lookupWorkflow(workflowId)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @GetMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<List<WorkflowStepRecord>> steps(
            @PathVariable UUID workflowId) {

        if (lookupWorkflow(workflowId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                workflows.trace(workflowId)
        );
    }

    @GetMapping("/workflows/{workflowId}/outputs/{role}")
    public ResponseEntity<WorkflowStepOutput> latestOutput(
            @PathVariable UUID workflowId,
            @PathVariable AgentRole role) {

        if (lookupWorkflow(workflowId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.of(
                workflows.latestOutput(workflowId, role)
        );
    }

    private Optional<WorkflowRunRecord> lookupWorkflow(
            UUID workflowId) {

        try {
            return Optional.of(
                    workflows.get(workflowId)
            );
        } catch (IllegalArgumentException ex) {
            // Persistence currently signals an unknown
            // workflow using IllegalArgumentException.
            return Optional.empty();
        }
    }
}
