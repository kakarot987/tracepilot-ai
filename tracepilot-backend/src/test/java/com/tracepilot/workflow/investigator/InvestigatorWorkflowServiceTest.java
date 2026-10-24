
package com.tracepilot.workflow.investigator;

import com.tracepilot.investigation.agent.AgentExecution;
import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.orchestration.FollowUpFocus;
import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowRunRecord;
import com.tracepilot.workflow.persistence.WorkflowStepRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvestigatorWorkflowServiceTest {

    private InvestigationAgent investigator;
    private InvestigationService investigations;
    private WorkflowPersistenceService workflows;
    private InvestigatorWorkflowService service;

    private UUID workflowId;
    private UUID stepId;
    private UUID investigationId;
    private WorkflowLease lease;

    @BeforeEach
    void setUp() {

        investigator = mock(InvestigationAgent.class);
        investigations = mock(InvestigationService.class);
        workflows = mock(WorkflowPersistenceService.class);

        // Initialize IDs FIRST.
        workflowId = UUID.randomUUID();
        stepId = UUID.randomUUID();
        investigationId = UUID.randomUUID();

        // Create the lease AFTER initializing workflowId.
        lease = new WorkflowLease(
                workflowId,
                UUID.randomUUID(),
                1L,
                Instant.now().plusSeconds(60)
        );

        service = new InvestigatorWorkflowService(
                investigator,
                investigations,
                workflows
        );

        when(workflows.get(workflowId))
                .thenReturn(new WorkflowRunRecord(
                        workflowId,
                        "INC-DB-001",
                        WorkflowStatus.RUNNING,
                        null,
                        1,
                        0,
                        1L,
                        Instant.now(),
                        null,
                        null
                ));

        when(workflows.beginStep(
                workflowId,
                AgentRole.INVESTIGATOR,
                lease
        )).thenReturn(new WorkflowStepRecord(
                stepId,
                workflowId,
                2,
                AgentRole.INVESTIGATOR,
                WorkflowStepStatus.RUNNING,
                Instant.now(),
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void completesSuccessfulInvestigationStep() {

        when(investigator.investigate(
                eq("INC-DB-001"),
                isNull(),
                eq(8)
        ))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.COMPLETED,
                        "Pool exhausted; LOG-DB-001",
                        2
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(
                workflowId, lease
        );

        assertEquals(
                InvestigationStatus.COMPLETED,
                result.status()
        );

        verify(workflows).beginStep(
                workflowId,
                AgentRole.INVESTIGATOR,
                lease
        );

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.INVESTIGATOR),
                contains("Investigation completed"),
                eq(result),
                eq(investigationId),
                eq(lease)
        );

        verify(workflows, never()).finishWorkflow(
                any(),
                any(),
                any()
        );

        verify(investigator).investigate(
                "INC-DB-001",
                null,
                8
        );
    }

    @Test
    void leavesWorkflowOpenWhenEvidenceInsufficient() {

        when(investigator.investigate(
                eq("INC-DB-001"),
                isNull(),
                eq(8)
        ))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        "",
                        1
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(
                workflowId, lease
        );

        assertEquals(
                InvestigationStatus.INSUFFICIENT_EVIDENCE,
                result.status()
        );

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.INVESTIGATOR),
                contains("Insufficient evidence"),
                eq(result),
                eq(investigationId),
                eq(lease)
        );

        verify(workflows, never()).finishWorkflow(
                any(),
                any(),
                any()
        );
    }

    @Test
    void terminatesWorkflowOnInvestigatorTimeout() {

        when(investigator.investigate(
                eq("INC-DB-001"),
                isNull(),
                eq(8)
        ))
                .thenReturn(new AgentExecution(
                        investigationId,
                        InvestigationStatus.TIMED_OUT,
                        "",
                        1
                ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(
                workflowId, lease
        );

        assertEquals(
                InvestigationStatus.TIMED_OUT,
                result.status()
        );

        verify(workflows).failStep(
                stepId,
                "INVESTIGATION_TIMEOUT",
                lease
        );

        verify(workflows).finishWorkflow(
                workflowId,
                WorkflowStatus.TIMED_OUT,
                lease
        );

        verify(workflows, never()).completeStepWithOutput(
                any(),
                any(),
                anyString(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void limitsFocusedFollowUpToFourToolCalls() {

        var focus = new FollowUpFocus(
                List.of("Inspect slow transactions")
        );

        when(investigator.investigate(
                eq("INC-DB-001"),
                eq(focus),
                eq(4)
        )).thenReturn(new AgentExecution(
                investigationId,
                InvestigationStatus.COMPLETED,
                "Follow-up investigation completed",
                4
        ));

        when(investigations.evidence(investigationId))
                .thenReturn(List.of());

        var result = service.execute(
                workflowId,
                focus,
                lease
        );

        assertEquals(
                4,
                result.toolCallsUsed()
        );

        verify(investigator).investigate(
                "INC-DB-001",
                focus,
                4
        );

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.INVESTIGATOR),
                contains("Investigation completed"),
                eq(result),
                eq(investigationId),
                eq(lease)
        );
    }

}
