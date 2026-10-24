
package com.tracepilot.workflow.persistence;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.Hypothesis;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.reviewer.ClaimReview;
import com.tracepilot.workflow.reviewer.ReviewDecision;
import com.tracepilot.workflow.reviewer.ReviewerResult;
import com.tracepilot.workflow.rootcause.RootCauseResult;
import com.tracepilot.workflow.triage.TriageCategory;
import com.tracepilot.workflow.triage.TriageResult;
import com.tracepilot.workflow.triage.TriageSeverity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
@Transactional
@Rollback
class WorkflowEndToEndPersistenceIntegrationTest {

    @Autowired
    private WorkflowPersistenceService workflows;

    @Autowired
    private WorkflowLeaseService leases;

    @Autowired
    private InvestigationService investigations;

    @Test
    void persistsCompleteFourAgentWorkflow() {

        // Start the parent workflow.
        var workflow = workflows.start("INC-DB-001");

        var lease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow();

        // STEP 1: TRIAGE
        var triageStep = workflows.beginStep(
                workflow.id(),
                AgentRole.TRIAGE,
                lease
        );

        var triageResult = new TriageResult(
                TriageSeverity.HIGH,
                TriageCategory.DATABASE,
                "Inspect database connection pool",
                List.of(
                        "Are active connections near capacity?"
                )
        );

        workflows.completeStepWithOutput(
                triageStep.id(),
                AgentRole.TRIAGE,
                "High severity database incident",
                triageResult,
                null,
                lease
        );

        // STEP 2: INVESTIGATOR
        // Create a real child investigation with
        // the same incident ID as the parent workflow.
        var investigation = investigations.start(
                "INC-DB-001"
        );

        investigations.finish(
                investigation.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        var investigatorStep = workflows.beginStep(
                workflow.id(),
                AgentRole.INVESTIGATOR,
                lease
        );

        workflows.completeStepWithOutput(
                investigatorStep.id(),
                AgentRole.INVESTIGATOR,
                "Investigation completed",
                Map.of(
                        "investigationRunId",
                        investigation.id().toString(),
                        "status",
                        "COMPLETED",
                        "evidenceIds",
                        List.of("LOG-DB-001")
                ),
                investigation.id(),
                lease
        );

        // STEP 3: ROOT CAUSE
        var rootCauseStep = workflows.beginStep(
                workflow.id(),
                AgentRole.ROOT_CAUSE,
                lease
        );

        var rootCauseResult = new RootCauseResult(
                List.of(new Hypothesis(
                        "Database connection pool exhausted",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "HIGH"
                )),
                List.of(),
                true
        );

        workflows.completeStepWithOutput(
                rootCauseStep.id(),
                AgentRole.ROOT_CAUSE,
                "One supported root-cause hypothesis",
                rootCauseResult,
                null,
                lease
        );

        // STEP 4: REVIEWER
        var reviewerStep = workflows.beginStep(
                workflow.id(),
                AgentRole.REVIEWER,
                lease
        );

        var reviewResult = new ReviewerResult(
                ReviewDecision.ACCEPT,
                List.of(new ClaimReview(
                        0,
                        true,
                        List.of("LOG-DB-001"),
                        "Log supports connection pool exhaustion"
                )),
                List.of(),
                "Evidence supports the proposed root cause"
        );

        workflows.completeStepWithOutput(
                reviewerStep.id(),
                AgentRole.REVIEWER,
                "Reviewer accepted the root cause",
                reviewResult,
                null,
                lease
        );

        // FINISH WORKFLOW
        workflows.finishWorkflow(
                workflow.id(),
                WorkflowStatus.COMPLETED,
                lease
        );

        // VERIFY PERSISTED PARENT STATE
        var persisted = workflows.get(workflow.id());

        assertEquals(
                WorkflowStatus.COMPLETED,
                persisted.status()
        );

        assertEquals(4, persisted.stepsUsed());
        assertEquals(0, persisted.followUpsUsed());
        assertNull(persisted.currentRole());
        assertNotNull(persisted.completedAt());

        // VERIFY ORDERED TRACE
        var trace = workflows.trace(workflow.id());

        assertEquals(4, trace.size());

        assertEquals(
                List.of(
                        AgentRole.TRIAGE,
                        AgentRole.INVESTIGATOR,
                        AgentRole.ROOT_CAUSE,
                        AgentRole.REVIEWER
                ),
                trace.stream()
                        .map(WorkflowStepRecord::agentRole)
                        .toList()
        );

        assertEquals(
                List.of(1, 2, 3, 4),
                trace.stream()
                        .map(WorkflowStepRecord::stepNumber)
                        .toList()
        );

        assertTrue(
                trace.stream().allMatch(
                        step -> step.status()
                                == WorkflowStepStatus.COMPLETED
                )
        );

        // VERIFY ALL FOUR JSONB OUTPUTS
        for (AgentRole role : List.of(
                AgentRole.TRIAGE,
                AgentRole.INVESTIGATOR,
                AgentRole.ROOT_CAUSE,
                AgentRole.REVIEWER
        )) {
            var output = workflows.latestOutput(
                    workflow.id(),
                    role
            ).orElseThrow();

            assertEquals(role, output.agentRole());
            assertEquals(1, output.schemaVersion());
            assertFalse(output.json().isBlank());
        }

        // VERIFY INVESTIGATOR CHILD RUN LINKAGE
        var investigatorOutput = workflows.output(
                investigatorStep.id()
        ).orElseThrow();

        assertEquals(
                investigation.id(),
                investigatorOutput.investigationRunId()
        );

        // VERIFY ROOT CAUSE EVIDENCE REFERENCE
        var rootCauseOutput = workflows.output(
                rootCauseStep.id()
        ).orElseThrow();

        assertTrue(
                rootCauseOutput.json().contains(
                        "LOG-DB-001"
                )
        );

        // VERIFY REVIEW DECISION
        var reviewerOutput = workflows.output(
                reviewerStep.id()
        ).orElseThrow();

        assertTrue(
                reviewerOutput.json().contains("ACCEPT")
        );
    }
}
