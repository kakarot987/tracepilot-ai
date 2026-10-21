
package com.tracepilot.workflow.orchestration;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.investigator.*;
import com.tracepilot.workflow.persistence.*;
import com.tracepilot.workflow.reviewer.*;
import com.tracepilot.workflow.rootcause.*;
import com.tracepilot.workflow.triage.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowOrchestratorTest {

    private WorkflowPersistenceService workflows;
    private TriageWorkflowService triage;
    private InvestigatorWorkflowService investigator;
    private RootCauseWorkflowService rootCause;
    private ReviewerWorkflowService reviewer;
    private ReviewEvidenceProvider evidenceProvider;

    private WorkflowOrchestrator orchestrator;
    private UUID workflowId;
    private WorkflowRunRecord running;
    private WorkflowLeaseService leases;

    @BeforeEach
    void setUp() {
        workflows = mock(WorkflowPersistenceService.class);
        triage = mock(TriageWorkflowService.class);
        investigator = mock(InvestigatorWorkflowService.class);
        rootCause = mock(RootCauseWorkflowService.class);
        reviewer = mock(ReviewerWorkflowService.class);
        evidenceProvider = mock(ReviewEvidenceProvider.class);
        leases = mock(WorkflowLeaseService.class);

        orchestrator = new WorkflowOrchestrator(
                workflows, triage, investigator,
                rootCause, reviewer, evidenceProvider,
                leases
        );

        workflowId = UUID.randomUUID();

        running = new WorkflowRunRecord(
                workflowId,
                "INC-DB-001",
                WorkflowStatus.RUNNING,
                null,
                1,
                0,
                1,
                Instant.now(),
                null,
                null
        );

        when(workflows.start("INC-DB-001"))
                .thenReturn(new WorkflowRunRecord(
                        workflowId,
                        "INC-DB-001",
                        WorkflowStatus.PENDING,
                        null,
                        0, 0, 0,
                        Instant.now(),
                        null, null
                ));

        when(workflows.get(workflowId))
                .thenReturn(running);
    }

    @Test
    void stopsBeforeRootCauseWhenEvidenceIsInsufficient() {

        when(investigator.execute(workflowId))
                .thenReturn(new InvestigatorStepResult(
                        UUID.randomUUID(),
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        "",
                        List.of(),
                        1
                ));

        var result = orchestrator.execute("INC-DB-001");

        verify(triage).execute(workflowId);

        verify(workflows).finishWorkflow(
                workflowId,
                WorkflowStatus.INSUFFICIENT_EVIDENCE,
                leases.acquire(
                        workflowId, UUID.randomUUID()
                ).orElseThrow()
        );

        verifyNoInteractions(rootCause);
        verifyNoInteractions(reviewer);
        verifyNoInteractions(evidenceProvider);

        assertEquals(workflowId, result.workflowId());
    }
}
