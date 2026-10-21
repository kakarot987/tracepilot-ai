
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
class WorkflowRecoveryIntegrationTest {

    @Autowired
    private WorkflowPersistenceService workflows;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private WorkflowLeaseService leases;

    private void expireWorkflow(
            java.util.UUID workflowId) {

        Instant oldStart = Instant.now()
                .minus(WorkflowLimits.MAX_DURATION)
                .minusSeconds(60);

        jdbc.sql("""
                UPDATE workflow_runs
                SET started_at = :startedAt
                WHERE id = :id
                """)
                .param("startedAt",
                        OffsetDateTime.ofInstant(
                                oldStart, ZoneOffset.UTC))
                .param("id", workflowId)
                .update();
    }

    @Test
    void expiresAbandonedRunningStep() {

        var run = workflows.start("INC-DB-001");

        var lease = leases.acquire(
                run.id(), UUID.randomUUID()
        ).orElseThrow();

        var step = workflows.beginStep(
                run.id(),
                AgentRole.INVESTIGATOR,
                lease
        );

        expireWorkflow(run.id());

        int recovered = workflows.recoverExpiredWorkflows();

        assertEquals(1, recovered);

        var persistedRun = workflows.get(run.id());
        var persistedStep = workflows.getStep(step.id());

        assertEquals(
                WorkflowStatus.TIMED_OUT,
                persistedRun.status()
        );

        assertNull(persistedRun.currentRole());
        assertNotNull(persistedRun.completedAt());

        assertEquals(
                WorkflowStepStatus.TIMED_OUT,
                persistedStep.status()
        );

        assertEquals(
                "WORKFLOW_DEADLINE_EXCEEDED",
                persistedStep.errorCode()
        );

        assertNotNull(persistedStep.completedAt());
        assertNotNull(persistedStep.durationMs());
    }

    @Test
    void recoveryIsIdempotent() {

        var run = workflows.start("INC-PAY-001");

        expireWorkflow(run.id());

        assertEquals(
                1,
                workflows.recoverExpiredWorkflows()
        );

        assertEquals(
                0,
                workflows.recoverExpiredWorkflows()
        );

        assertEquals(
                WorkflowStatus.TIMED_OUT,
                workflows.get(run.id()).status()
        );
    }

    @Test
    void rejectsLateAgentCompletion() {

        var run = workflows.start("INC-MEM-001");

        var lease = leases.acquire(
                run.id(), UUID.randomUUID()
        ).orElseThrow();

        var step = workflows.beginStep(
                run.id(),
                AgentRole.REVIEWER,
                lease
        );

        expireWorkflow(run.id());

        assertThrows(
                IllegalStateException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.REVIEWER,
                        "Late accepted result",
                        Map.of("decision", "ACCEPT"),
                        null,
                        lease
                )
        );

        assertTrue(workflows.output(step.id()).isEmpty());

        assertEquals(
                WorkflowStepStatus.RUNNING,
                workflows.getStep(step.id()).status()
        );

        workflows.recoverExpiredWorkflows();

        assertEquals(
                WorkflowStatus.TIMED_OUT,
                workflows.get(run.id()).status()
        );
    }

    @Test
    void rejectsNewStepAfterDeadline() {

        var run = workflows.start("INC-RATE-001");
        var lease = leases.acquire(
                run.id(), UUID.randomUUID()
        ).orElseThrow();
        expireWorkflow(run.id());

        assertThrows(
                IllegalStateException.class,
                () -> workflows.beginStep(
                        run.id(),
                        AgentRole.TRIAGE,
                        lease
                )
        );
    }

    @Test
    void doesNotExpireFreshWorkflow() {

        var run = workflows.start("INC-PAY-001");

        int recovered = workflows.recoverExpiredWorkflows();

        assertEquals(0, recovered);

        assertEquals(
                WorkflowStatus.PENDING,
                workflows.get(run.id()).status()
        );
    }
}
