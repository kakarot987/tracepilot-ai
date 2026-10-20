
package com.tracepilot.workflow.persistence;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.rootcause.RootCauseResult;
import com.tracepilot.workflow.domain.Hypothesis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    WorkflowPersistenceService workflows;

    @Autowired
    InvestigationService investigations;

    @Test
    void persistsRootCauseJsonAndReloadsIt() {
        var workflow = workflows.start("INC-DB-001");

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.ROOT_CAUSE
        );

        var result = new RootCauseResult(
                List.of(new Hypothesis(
                        "Database connection saturation",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of("Why did connections accumulate?"),
                false
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.ROOT_CAUSE,
                "One provisional hypothesis",
                result,
                null
        );

        var persisted = workflows.output(step.id())
                .orElseThrow();

        assertEquals(AgentRole.ROOT_CAUSE,
                persisted.agentRole());

        assertEquals(1, persisted.schemaVersion());

        assertNull(persisted.investigationRunId());

        assertTrue(persisted.json().contains(
                "Database connection saturation"));

        assertTrue(persisted.json().contains(
                "LOG-DB-001"));

        var latest = workflows.latestOutput(
                workflow.id(),
                AgentRole.ROOT_CAUSE
        ).orElseThrow();

        assertEquals(step.id(), latest.stepId());
    }

    @Test
    void linksInvestigatorStepToCorrectChildRun() {
        var workflow = workflows.start("INC-PAY-001");

        var investigation = investigations.start(
                "INC-PAY-001");

        investigations.finish(
                investigation.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.INVESTIGATOR
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.INVESTIGATOR,
                "Investigation completed",
                new java.util.HashMap<>(java.util.Map.of(
                        "runId", investigation.id().toString(),
                        "status", "COMPLETED"
                )),
                investigation.id()
        );

        var persisted = workflows.output(step.id())
                .orElseThrow();

        assertEquals(
                investigation.id(),
                persisted.investigationRunId()
        );
    }

    @Test
    void rejectsChildRunFromAnotherIncident() {
        var workflow = workflows.start("INC-DB-001");

        var otherInvestigation = investigations.start(
                "INC-PAY-001");

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.INVESTIGATOR
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.INVESTIGATOR,
                        "Invalid linkage",
                        java.util.Map.of(
                                "status", "COMPLETED"
                        ),
                        otherInvestigation.id()
                )
        );

        assertTrue(workflows.output(step.id()).isEmpty());
    }

    @Test
    void rejectsDuplicateOutputCompletion() {
        var workflow = workflows.start("INC-MEM-001");

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.REVIEWER
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.REVIEWER,
                "Review completed",
                java.util.Map.of(
                        "decision", "FOLLOW_UP"
                ),
                null
        );

        assertThrows(
                IllegalStateException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.REVIEWER,
                        "Duplicate",
                        java.util.Map.of(
                                "decision", "ACCEPT"
                        ),
                        null
                )
        );
    }
}
