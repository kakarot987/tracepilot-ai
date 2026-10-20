
package com.tracepilot.workflow.reviewer;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.Hypothesis;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.persistence.WorkflowRunRecord;
import com.tracepilot.workflow.persistence.WorkflowStepRecord;
import com.tracepilot.workflow.rootcause.RootCauseResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewerWorkflowServiceTest {

    private ReviewerAgent reviewer;
    private WorkflowPersistenceService workflows;
    private ReviewerWorkflowService service;

    private UUID workflowId;
    private UUID stepId;

    @BeforeEach
    void setUp() {
        reviewer = mock(ReviewerAgent.class);
        workflows = mock(WorkflowPersistenceService.class);

        service = new ReviewerWorkflowService(
                reviewer,
                workflows
        );

        workflowId = UUID.randomUUID();
        stepId = UUID.randomUUID();

        when(workflows.get(workflowId))
                .thenReturn(new WorkflowRunRecord(
                        workflowId,
                        "INC-DB-001",
                        WorkflowStatus.RUNNING,
                        null,
                        3,
                        0,
                        3L,
                        Instant.now(),
                        null,
                        null
                ));

        when(workflows.beginStep(
                workflowId,
                AgentRole.REVIEWER
        )).thenReturn(new WorkflowStepRecord(
                stepId,
                workflowId,
                4,
                AgentRole.REVIEWER,
                WorkflowStepStatus.RUNNING,
                Instant.now(),
                null,
                null,
                null,
                null
        ));
    }

    private RootCauseResult rootCause() {
        return new RootCauseResult(
                List.of(new Hypothesis(
                        "Connection pool exhaustion",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of(),
                true
        );
    }

    private List<EvidenceExcerpt> evidence() {
        return List.of(new EvidenceExcerpt(
                "LOG-DB-001",
                "LOG",
                "Database connection pool reached capacity."
        ));
    }

    @Test
    void persistsSuccessfulReviewerStep() {
        var result = new ReviewerResult(
                ReviewDecision.ACCEPT,
                List.of(new ClaimReview(
                        0,
                        true,
                        List.of("LOG-DB-001"),
                        "Current log supports pool saturation."
                )),
                List.of(),
                "All hypotheses reviewed."
        );

        when(reviewer.review(any(), anyList()))
                .thenReturn(result);

        ReviewerResult actual = service.execute(
                workflowId,
                rootCause(),
                evidence()
        );

        assertEquals(ReviewDecision.ACCEPT,
                actual.decision());

        verify(workflows).beginStep(
                workflowId, AgentRole.REVIEWER);

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.REVIEWER),
                contains("ACCEPT"),
                any(ReviewerResult.class),
                isNull()
        );

        verify(workflows, never()).finishWorkflow(
                any(), any());
    }

    @Test
    void leavesWorkflowOpenAfterFollowUpDecision() {
        var result = new ReviewerResult(
                ReviewDecision.FOLLOW_UP,
                List.of(new ClaimReview(
                        0,
                        false,
                        List.of("LOG-DB-001"),
                        "The underlying cause remains uncertain."
                )),
                List.of("Inspect transaction durations"),
                "Additional evidence required."
        );

        when(reviewer.review(any(), anyList()))
                .thenReturn(result);

        ReviewerResult actual = service.execute(
                workflowId,
                rootCause(),
                evidence()
        );

        assertEquals(ReviewDecision.FOLLOW_UP,
                actual.decision());

        verify(workflows).completeStepWithOutput(
                eq(stepId),
                eq(AgentRole.REVIEWER),
                contains("FOLLOW_UP"),
                any(ReviewerResult.class),
                isNull()
        );

        verify(workflows, never()).finishWorkflow(
                any(), any());
    }

    @Test
    void persistsFailureWhenReviewerThrows() {
        when(reviewer.review(any(), anyList()))
                .thenThrow(
                        new IllegalArgumentException(
                                "Malformed reviewer response")
                );

        when(workflows.getStep(stepId))
                .thenReturn(new WorkflowStepRecord(
                        stepId,
                        workflowId,
                        4,
                        AgentRole.REVIEWER,
                        WorkflowStepStatus.RUNNING,
                        Instant.now(),
                        null,
                        null,
                        null,
                        null
                ));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        workflowId,
                        rootCause(),
                        evidence()
                )
        );

        verify(workflows).failStep(
                stepId, "REVIEWER_FAILED");

        verify(workflows).finishWorkflow(
                workflowId, WorkflowStatus.FAILED);
    }

    @Test
    void refusesTerminalWorkflow() {
        when(workflows.get(workflowId))
                .thenReturn(new WorkflowRunRecord(
                        workflowId,
                        "INC-DB-001",
                        WorkflowStatus.COMPLETED,
                        null,
                        4,
                        0,
                        4L,
                        Instant.now(),
                        Instant.now(),
                        null
                ));

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(
                        workflowId,
                        rootCause(),
                        evidence()
                )
        );

        verify(workflows, never()).beginStep(
                any(), any());
        verifyNoInteractions(reviewer);
    }
}
