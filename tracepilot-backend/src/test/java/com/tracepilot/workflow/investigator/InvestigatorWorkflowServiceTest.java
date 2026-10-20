
package com.tracepilot.workflow.investigator;

import com.tracepilot.investigation.agent.AgentExecution;
import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowRunRecord;
import com.tracepilot.workflow.persistence.WorkflowStepRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigatorWorkflowServiceTest {

    private InvestigationAgent investigator;
    private InvestigationService investigations;
    private WorkflowPersistenceService workflows;
    private InvestigatorWorkflowService service;

    private final UUID workflowId = UUID.randomUUID();
    private final UUID stepId = UUID.randomUUID();
    private final UUID investigationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        investigator = mock(InvestigationAgent.class);
        investigations = mock(InvestigationService.class);
        workflows = mock(WorkflowPersistenceService.class);

        service = new InvestigatorWorkflowService(
                investigator, investigations, workflows
        );

        when(workflows.get(workflowId))
                .thenReturn(new WorkflowRunRecord(
                        workflowId,
                        "INC-DB-001",
                        WorkflowStatus.RUNNING,
                        null,
                        1, 0, 1L,
                        Instant.now(),
                        null, null
                ));

        when(workflows.beginStep(
                workflowId, AgentRole.INVESTIGATOR))
                .thenReturn(new WorkflowStepRecord(
                        stepId, workflowId, 2,
                        AgentRole.INVESTIGATOR,
                        com.tracepilot.workflow.domain
                                .WorkflowStepStatus.RUNNING,
                        Instant.now(),
                        null, null, null, null
                ));
    }

    @Test
    void completesSuccessfulInvestigationStep() {

        when(investigator.investigate("INC-DB-001"))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.COMPLETED,
                        "Pool exhausted; LOG-DB-001",
                        2
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(workflowId);

        assertEquals(
                InvestigationStatus.COMPLETED,
                result.status()
        );

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.INVESTIGATOR),
                contains("Investigation completed"),
                eq(result),
                eq(investigationId)
        );

        verify(workflows, never()).finishWorkflow(
                any(), any()
        );
    }


    @Test
    void leavesWorkflowOpenWhenEvidenceInsufficient() {

        when(investigator.investigate("INC-DB-001"))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        "",
                        1
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(workflowId);

        assertEquals(
                InvestigationStatus.INSUFFICIENT_EVIDENCE,
                result.status()
        );

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.INVESTIGATOR),
                contains("Insufficient evidence"),
                eq(result),
                eq(investigationId)
        );

        verify(workflows, never()).finishWorkflow(
                any(), any()
        );
    }

    @Test
    void terminatesWorkflowOnInvestigatorTimeout() {

        when(investigator.investigate("INC-DB-001"))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.TIMED_OUT,
                        "",
                        1
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        service.execute(workflowId);

        verify(workflows).failStep(
                stepId, "INVESTIGATION_TIMEOUT");

        verify(workflows).finishWorkflow(
                workflowId, WorkflowStatus.TIMED_OUT);
    }
}
