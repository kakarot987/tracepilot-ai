
package com.tracepilot.workflow.persistence;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.Hypothesis;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.rootcause.RootCauseResult;

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
class WorkflowHandoffIntegrationTest {

    @Autowired
    private WorkflowPersistenceService workflows;

    @Autowired
    private InvestigationService investigations;

    @Autowired
    private WorkflowLeaseService leases;

    private WorkflowLease acquireLease(UUID workflowId) {
        return leases.acquire(
                workflowId,
                UUID.randomUUID()
        ).orElseThrow();
    }

    @Test
    void persistsRootCauseJsonAndReloadsIt() {

        var workflow = workflows.start("INC-DB-001");
        var lease = acquireLease(workflow.id());

        // Create an actual ROOT_CAUSE step,
        // not a completed TRIAGE step.
        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.ROOT_CAUSE,
                lease
        );

        var result = new RootCauseResult(
                List.of(new Hypothesis(
                        "Database connection saturation",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of(
                        "Why did connections accumulate?"
                ),
                false
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.ROOT_CAUSE,
                "One provisional hypothesis",
                result,
                null,
                lease
        );

        var persisted = workflows.output(step.id())
                .orElseThrow();

        assertEquals(
                AgentRole.ROOT_CAUSE,
                persisted.agentRole()
        );

        assertEquals(
                1,
                persisted.schemaVersion()
        );

        assertNull(
                persisted.investigationRunId()
        );

        assertTrue(
                persisted.json().contains(
                        "Database connection saturation"
                )
        );

        assertTrue(
                persisted.json().contains("LOG-DB-001")
        );

        var latest = workflows.latestOutput(
                workflow.id(),
                AgentRole.ROOT_CAUSE
        ).orElseThrow();

        assertEquals(
                step.id(),
                latest.stepId()
        );

        assertEquals(
                WorkflowStepStatus.COMPLETED,
                workflows.getStep(step.id()).status()
        );
    }

    @Test
    void linksInvestigatorStepToCorrectChildRun() {

        var workflow = workflows.start("INC-PAY-001");
        var lease = acquireLease(workflow.id());

        var investigation = investigations.start(
                "INC-PAY-001"
        );

        investigations.finish(
                investigation.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.INVESTIGATOR,
                lease
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.INVESTIGATOR,
                "Investigation completed",
                Map.of(
                        "runId",
                        investigation.id().toString(),
                        "status",
                        "COMPLETED"
                ),
                investigation.id(),
                lease
        );

        var persisted = workflows.output(step.id())
                .orElseThrow();

        assertEquals(
                investigation.id(),
                persisted.investigationRunId()
        );

        assertEquals(
                AgentRole.INVESTIGATOR,
                persisted.agentRole()
        );

        assertEquals(
                1,
                persisted.schemaVersion()
        );

        assertTrue(
                persisted.json().contains(
                        investigation.id().toString()
                )
        );
    }

    @Test
    void rejectsChildRunFromAnotherIncident() {

        var workflow = workflows.start("INC-DB-001");
        var lease = acquireLease(workflow.id());

        var otherInvestigation = investigations.start(
                "INC-PAY-001"
        );

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.INVESTIGATOR,
                lease
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.INVESTIGATOR,
                        "Invalid linkage",
                        Map.of(
                                "status",
                                "COMPLETED"
                        ),
                        otherInvestigation.id(),
                        lease
                )
        );

        assertTrue(
                workflows.output(step.id()).isEmpty()
        );

        assertEquals(
                WorkflowStepStatus.RUNNING,
                workflows.getStep(step.id()).status()
        );
    }

    @Test
    void rejectsDuplicateOutputCompletion() {

        var workflow = workflows.start("INC-MEM-001");
        var lease = acquireLease(workflow.id());

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.REVIEWER,
                lease
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.REVIEWER,
                "Review completed",
                Map.of(
                        "decision",
                        "FOLLOW_UP"
                ),
                null,
                lease
        );

        assertThrows(
                IllegalStateException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.REVIEWER,
                        "Duplicate",
                        Map.of(
                                "decision",
                                "ACCEPT"
                        ),
                        null,
                        lease
                )
        );

        // The original output must remain unchanged.
        var persisted = workflows.output(
                step.id()
        ).orElseThrow();

        assertTrue(
                persisted.json().contains(
                        "FOLLOW_UP"
                )
        );

        assertFalse(
                persisted.json().contains(
                        "ACCEPT"
                )
        );

        assertEquals(
                WorkflowStepStatus.COMPLETED,
                workflows.getStep(step.id()).status()
        );
    }
}
