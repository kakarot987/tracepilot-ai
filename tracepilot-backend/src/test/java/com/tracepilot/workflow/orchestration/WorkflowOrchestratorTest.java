
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
    private WorkflowLease lease;
    private WorkflowHeartbeatService heartbeats;
    private WorkflowHeartbeatService.Heartbeat heartbeat;

    @BeforeEach
    void setUp() {

        workflows = mock(WorkflowPersistenceService.class);
        triage = mock(TriageWorkflowService.class);
        investigator = mock(InvestigatorWorkflowService.class);
        rootCause = mock(RootCauseWorkflowService.class);
        reviewer = mock(ReviewerWorkflowService.class);
        evidenceProvider = mock(ReviewEvidenceProvider.class);
        leases = mock(WorkflowLeaseService.class);
        heartbeats = mock(WorkflowHeartbeatService.class);

        // Initialize IDs before creating the lease.
        workflowId = UUID.randomUUID();

        lease = new WorkflowLease(
                workflowId,
                UUID.randomUUID(),
                1L,
                Instant.now().plusSeconds(60)
        );

        heartbeat = mock(
                WorkflowHeartbeatService.Heartbeat.class
        );

        // Stub using the initialized lease.
        when(heartbeats.start(lease))
                .thenReturn(heartbeat);

        when(leases.acquire(
                eq(workflowId),
                any(UUID.class)
        )).thenReturn(
                java.util.Optional.of(lease)
        );

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

        orchestrator = new WorkflowOrchestrator(
                workflows,
                triage,
                investigator,
                rootCause,
                reviewer,
                evidenceProvider,
                leases,
                heartbeats
        );
    }

    @Test
    void stopsBeforeRootCauseWhenEvidenceIsInsufficient() {

        when(investigator.execute(workflowId, lease))
                .thenReturn(new InvestigatorStepResult(
                        UUID.randomUUID(),
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        "",
                        List.of(),
                        1
                ));

        var result = orchestrator.execute("INC-DB-001");

        verify(triage).execute(workflowId, lease);

        verify(workflows).finishWorkflow(
                workflowId,
                WorkflowStatus.INSUFFICIENT_EVIDENCE,
                lease
        );

        verifyNoInteractions(rootCause);
        verifyNoInteractions(reviewer);
        verifyNoInteractions(evidenceProvider);

        assertEquals(workflowId, result.workflowId());

        verify(heartbeats).start(lease);

        verify(heartbeat, atLeastOnce()).assertHealthy();

        verify(heartbeat).close();

        verify(leases, times(1)).release(lease);
    }
}
