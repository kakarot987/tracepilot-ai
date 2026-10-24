
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
    private WorkflowExecutionStarter starter;

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
        starter = mock(WorkflowExecutionStarter.class);
        // Initialize IDs before creating the lease.
        workflowId = UUID.randomUUID();

        lease = new WorkflowLease(
                workflowId,
                UUID.randomUUID(),
                1L,
                Instant.now().plusSeconds(60)
        );
        when(starter.start("INC-DB-001")).thenReturn(lease);
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
                heartbeats,
                starter
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

    @Test
    void completesWorkflowWhenReviewerAcceptsEvidence() {

        UUID investigationId = UUID.randomUUID();

        var investigation = new InvestigatorStepResult(
                investigationId,
                InvestigationStatus.COMPLETED,
                "Database connection pool exhausted",
                List.of("LOG-DB-001"),
                3
        );

        var hypothesis = new com.tracepilot.workflow.domain.Hypothesis(
                "Database connections were exhausted",
                List.of("LOG-DB-001"),
                List.of(),
                "HIGH"
        );

        var analysis = new RootCauseResult(
                List.of(hypothesis),
                List.of(),
                true
        );

        var excerpt = new EvidenceExcerpt(
                "LOG-DB-001",
                "LOG",
                "Connection pool reached maximum capacity"
        );

        var review = new ReviewerResult(
                ReviewDecision.ACCEPT,
                List.of(new ClaimReview(
                        0,
                        true,
                        List.of("LOG-DB-001"),
                        "Evidence supports connection pool exhaustion"
                )),
                List.of(),
                "Root cause is supported by incident evidence"
        );

        when(investigator.execute(workflowId, lease))
                .thenReturn(investigation);

        when(rootCause.execute(
                workflowId,
                investigation,
                lease
        )).thenReturn(analysis);

        when(evidenceProvider.load(
                investigationId,
                List.of("LOG-DB-001")
        )).thenReturn(List.of(excerpt));

        when(reviewer.execute(
                workflowId,
                analysis,
                List.of(excerpt),
                lease
        )).thenReturn(review);

        // Simulate persistence reflecting the terminal state
        // after the orchestrator finishes the workflow.
        doAnswer(invocation -> {

            var completed = new WorkflowRunRecord(
                    workflowId,
                    "INC-DB-001",
                    WorkflowStatus.COMPLETED,
                    null,
                    4,
                    0,
                    5L,
                    running.startedAt(),
                    Instant.now(),
                    null
            );

            when(workflows.get(workflowId))
                    .thenReturn(completed);

            return null;

        }).when(workflows).finishWorkflow(
                workflowId,
                WorkflowStatus.COMPLETED,
                lease
        );

        var result = orchestrator.execute("INC-DB-001");

        assertEquals(
                WorkflowStatus.COMPLETED,
                result.status()
        );

        assertEquals(workflowId, result.workflowId());
        assertEquals(0, result.followUpsUsed());
        assertEquals(analysis, result.rootCause());
        assertEquals(review, result.review());

        var order = inOrder(
                triage,
                investigator,
                rootCause,
                evidenceProvider,
                reviewer
        );

        order.verify(triage).execute(workflowId, lease);

        order.verify(investigator)
                .execute(workflowId, lease);

        order.verify(rootCause).execute(
                workflowId,
                investigation,
                lease
        );

        order.verify(evidenceProvider).load(
                investigationId,
                List.of("LOG-DB-001")
        );

        order.verify(reviewer).execute(
                workflowId,
                analysis,
                List.of(excerpt),
                lease
        );

        verify(workflows).finishWorkflow(
                workflowId,
                WorkflowStatus.COMPLETED,
                lease
        );

        verify(heartbeats).start(lease);
        verify(heartbeat, atLeastOnce()).assertHealthy();
        verify(heartbeat).close();
        verify(leases, times(1)).release(lease);

        verify(workflows, never()).reserveFollowUp(
                any(UUID.class),
                any(WorkflowLease.class)
        );
    }

    @Test
    void followUpWithOnlyPreviouslySeenEvidenceStopsBeforeAnotherRootCause() {
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var original = new InvestigatorStepResult(
                firstId, InvestigationStatus.COMPLETED,
                "Connection timeout [LOG-DB-003]",
                List.of("LOG-DB-003"), 1);
        var repeated = new InvestigatorStepResult(
                secondId, InvestigationStatus.COMPLETED,
                "Same connection timeout [LOG-DB-003]",
                List.of("LOG-DB-003"), 1);
        var hypothesis = new com.tracepilot.workflow.domain.Hypothesis(
                "Database connection acquisition timed out",
                List.of("LOG-DB-003"), List.of(), "LOW");
        var analysis = new RootCauseResult(
                List.of(hypothesis), List.of("What caused it?"), false);
        var excerpt = new EvidenceExcerpt(
                "LOG-DB-003", "LOG", "Unable to acquire JDBC connection");
        var followUpReview = new ReviewerResult(
                ReviewDecision.FOLLOW_UP,
                List.of(new ClaimReview(0, false, List.of("LOG-DB-003"),
                        "Cause unproven")),
                List.of("What caused connection saturation?"),
                "Need independent operational evidence");

        when(investigator.execute(workflowId, lease)).thenReturn(original);
        when(investigator.execute(
                eq(workflowId), any(FollowUpFocus.class), eq(lease)))
                .thenReturn(repeated);
        when(rootCause.execute(workflowId, original, lease))
                .thenReturn(analysis);
        when(evidenceProvider.load(firstId, List.of("LOG-DB-003")))
                .thenReturn(List.of(excerpt));
        when(reviewer.execute(workflowId, analysis, List.of(excerpt), lease))
                .thenReturn(followUpReview);
        when(workflows.reserveFollowUp(workflowId, lease))
                .thenReturn(true);

        orchestrator.execute("INC-DB-001");

        verify(investigator).execute(
                eq(workflowId), any(FollowUpFocus.class), eq(lease));
        verify(rootCause, times(1)).execute(
                any(UUID.class), any(InvestigatorStepResult.class),
                eq(lease));
        verify(reviewer, times(1)).execute(
                any(UUID.class), any(RootCauseResult.class),
                anyList(), eq(lease));
        verify(workflows).finishWorkflow(
                workflowId, WorkflowStatus.INSUFFICIENT_EVIDENCE, lease);
    }

}
