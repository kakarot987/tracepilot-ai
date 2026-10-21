
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class WorkflowPersistenceIntegrationTest {

    @Autowired
    WorkflowPersistenceService workflows;

    @Autowired
    WorkflowLeaseService leases;

    @Test
    void persistsChronologicalAgentTrace() {

        var run = workflows.start("INC-DB-001");
        var lease = leases.acquire(
                run.id(), UUID.randomUUID()
        ).orElseThrow();

        assertEquals(WorkflowStatus.PENDING, run.status());

        var triage = workflows.beginStep(
                run.id(), AgentRole.TRIAGE, lease);

        workflows.completeStep(
                triage.id(), "Database connection issue suspected", lease);

        var investigation = workflows.beginStep(
                run.id(), AgentRole.INVESTIGATOR, lease);

        workflows.completeStep(
                investigation.id(), "Current logs examined", lease);

        var trace = workflows.trace(run.id());

        assertEquals(2, trace.size());
        assertEquals(1, trace.get(0).stepNumber());
        assertEquals(2, trace.get(1).stepNumber());
        assertEquals(AgentRole.TRIAGE,
                trace.get(0).agentRole());
        assertEquals(AgentRole.INVESTIGATOR,
                trace.get(1).agentRole());

        assertTrue(trace.stream().allMatch(step ->
                step.status() == WorkflowStepStatus.COMPLETED
                        && step.completedAt() != null
                        && step.durationMs() >= 0));

        workflows.finishWorkflow(
                run.id(), WorkflowStatus.COMPLETED, lease);

        assertEquals(WorkflowStatus.COMPLETED,
                workflows.get(run.id()).status());
    }

    @Test
    void rejectsDoubleCompletion() {

        var run = workflows.start("INC-PAY-001");
        var step = workflows.beginStep(
                run.id(),
                AgentRole.TRIAGE,
                leases.acquire(run.id(), UUID.randomUUID()).orElseThrow());

        workflows.completeStep(step.id(), "Triage done", leases.acquire(run.id(), UUID.randomUUID()).orElseThrow());

        assertThrows(
                IllegalStateException.class,
                () -> workflows.completeStep(
                        step.id(), "Duplicate completion", leases.acquire(run.id(), UUID.randomUUID()).orElseThrow()));
    }

    @Test
    void rejectsStartingStepInTerminalWorkflow() {

        var run = workflows.start("INC-MEM-001");

        var lease = leases.acquire(
                run.id(), UUID.randomUUID()
        ).orElseThrow();

        workflows.finishWorkflow(
                run.id(),
                WorkflowStatus.CANCELLED,
                lease
        );

        assertThrows(
                IllegalStateException.class,
                () -> workflows.beginStep(
                        run.id(), AgentRole.TRIAGE, lease));
    }

    @Test
    void rejectsFinalizingActiveWorkflow() {

        var run = workflows.start("INC-RATE-001");

        workflows.beginStep(
                run.id(), AgentRole.INVESTIGATOR, leases.acquire(run.id(), UUID.randomUUID()).orElseThrow());

        assertThrows(
                IllegalStateException.class,
                () -> workflows.finishWorkflow(
                        run.id(), WorkflowStatus.COMPLETED,
                        leases.acquire(run.id(), UUID.randomUUID()).orElseThrow(IllegalStateException::new)));
    }

    @Test
    void unknownWorkflowFails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workflows.get(UUID.randomUUID()));
    }
}
