
package com.tracepilot.workflow.api;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.orchestration.WorkflowExecutionResult;
import com.tracepilot.workflow.orchestration.WorkflowOrchestrator;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowRunRecord;
import com.tracepilot.workflow.persistence.WorkflowStepOutput;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowControllerTest {

    private WorkflowOrchestrator orchestrator;
    private WorkflowPersistenceService workflows;
    private IncidentRepository incidents;
    private WorkflowController controller;

    private UUID workflowId;
    private WorkflowRunRecord run;

    @BeforeEach
    void setUp() {

        orchestrator = mock(WorkflowOrchestrator.class);
        workflows = mock(WorkflowPersistenceService.class);
        incidents = mock(IncidentRepository.class);

        controller = new WorkflowController(
                orchestrator,
                workflows,
                incidents
        );

        workflowId = UUID.randomUUID();

        run = new WorkflowRunRecord(
                workflowId,
                "INC-DB-001",
                WorkflowStatus.COMPLETED,
                null,
                4,
                0,
                9L,
                Instant.now().minusSeconds(30),
                Instant.now(),
                null
        );
    }

    @Test
    void startsWorkflowForExistingIncident() {

        // The controller only checks whether the
        // Optional is nonempty. A mock Incident is enough.
        var incident = mock(
                com.tracepilot.incident.domain.Incident.class
        );

        when(incidents.findById("INC-DB-001"))
                .thenReturn(Optional.of(incident));

        var execution = new WorkflowExecutionResult(
                workflowId,
                WorkflowStatus.COMPLETED,
                0,
                null,
                null
        );

        when(orchestrator.execute("INC-DB-001"))
                .thenReturn(execution);

        var response = controller.start("INC-DB-001");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                execution,
                response.getBody()
        );

        verify(orchestrator).execute("INC-DB-001");
    }

    @Test
    void rejectsUnknownIncidentWithoutStartingWorkflow() {

        when(incidents.findById("INC-MISSING"))
                .thenReturn(Optional.empty());

        var response = controller.start("INC-MISSING");

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        verifyNoInteractions(orchestrator);
    }

    @Test
    void returnsPersistedWorkflow() {

        when(workflows.get(workflowId))
                .thenReturn(run);

        var response = controller.get(workflowId);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(run, response.getBody());
    }

    @Test
    void returns404ForUnknownWorkflow() {

        when(workflows.get(workflowId))
                .thenThrow(
                        new IllegalArgumentException(
                                "Unknown workflow"
                        )
                );

        var response = controller.get(workflowId);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );
    }

    @Test
    void returnsEmptyTraceForWorkflowWithoutSteps() {

        when(workflows.get(workflowId))
                .thenReturn(run);

        when(workflows.trace(workflowId))
                .thenReturn(List.of());

        var response = controller.steps(workflowId);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                List.of(),
                response.getBody()
        );
    }

    @Test
    void returnsLatestPersistedAgentOutput() {

        when(workflows.get(workflowId))
                .thenReturn(run);

        var output = new WorkflowStepOutput(
                UUID.randomUUID(),
                workflowId,
                AgentRole.REVIEWER,
                1,
                "{\"decision\":\"ACCEPT\"}",
                null
        );

        when(workflows.latestOutput(
                workflowId,
                AgentRole.REVIEWER
        )).thenReturn(Optional.of(output));

        var response = controller.latestOutput(
                workflowId,
                AgentRole.REVIEWER
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(output, response.getBody());
    }

    @Test
    void returns404WhenRequestedAgentOutputIsMissing() {

        when(workflows.get(workflowId))
                .thenReturn(run);

        when(workflows.latestOutput(
                workflowId,
                AgentRole.ROOT_CAUSE
        )).thenReturn(Optional.empty());

        var response = controller.latestOutput(
                workflowId,
                AgentRole.ROOT_CAUSE
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );
    }
}
