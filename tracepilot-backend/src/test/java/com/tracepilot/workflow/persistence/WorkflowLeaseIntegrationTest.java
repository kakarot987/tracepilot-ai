
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;

import com.tracepilot.workflow.domain.WorkflowStepStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
class WorkflowLeaseIntegrationTest {

    @Autowired
    WorkflowPersistenceService workflows;

    @Autowired
    WorkflowLeaseService leases;

    @Autowired
    JdbcClient jdbc;

    @Test
    void onlyOneExecutorAcquiresActiveLease() {
        var workflow = workflows.start("INC-PAY-001");

        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();

        var first = leases.acquire(
                workflow.id(), ownerA
        ).orElseThrow();

        assertEquals(ownerA, first.ownerId());
        assertTrue(leases.isOwner(first));

        assertTrue(
                leases.acquire(workflow.id(), ownerB).isEmpty()
        );
    }

    @Test
    void expiredLeaseCanBeTakenOver() {
        var workflow = workflows.start("INC-DB-001");

        var oldLease = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        // Simulate expiration without sleeping.
        jdbc.sql("""
                UPDATE workflow_runs
                SET lease_expires_at =
                    clock_timestamp() - INTERVAL '1 second'
                WHERE id = :id
                """)
                .param("id", workflow.id())
                .update();

        var newLease = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        assertTrue(
                newLease.fencingToken()
                        > oldLease.fencingToken()
        );

        assertFalse(leases.isOwner(oldLease));
        assertTrue(leases.isOwner(newLease));
        assertFalse(leases.renew(oldLease));
        assertFalse(leases.release(oldLease));
    }

    @Test
    void oldOwnerCannotReleaseNewOwnerLease() {
        var workflow = workflows.start("INC-MEM-001");

        var oldLease = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        jdbc.sql("""
                UPDATE workflow_runs
                SET lease_expires_at =
                    clock_timestamp() - INTERVAL '1 second'
                WHERE id = :id
                """)
                .param("id", workflow.id())
                .update();

        var newLease = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        assertFalse(leases.release(oldLease));
        assertTrue(leases.isOwner(newLease));

        assertTrue(leases.release(newLease));
    }

    @Test
    void releasedLeaseCanBeAcquiredAgain() {
        var workflow = workflows.start("INC-RATE-001");

        var first = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        assertTrue(leases.release(first));

        var second = leases.acquire(
                workflow.id(), UUID.randomUUID()
        ).orElseThrow();

        assertTrue(
                second.fencingToken() > first.fencingToken()
        );
    }

    @Test
    void terminalWorkflowCannotBeAcquired() {
        var workflow = workflows.start("INC-PAY-001");

        workflows.finishWorkflow(
                workflow.id(), WorkflowStatus.CANCELLED,
                leases.acquire(
                        workflow.id(),
                        UUID.randomUUID()
                ).orElseThrow()
        );

        assertTrue(
                leases.acquire(
                        workflow.id(),
                        UUID.randomUUID()
                ).isEmpty()
        );
    }


    @Test
    void rejectsStaleExecutorAfterLeaseTakeover() {

        var workflow = workflows.start("INC-DB-001");

        var oldLease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow();

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.REVIEWER,
                oldLease
        );

        // Simulate lease expiration without sleeping.
        jdbc.sql("""
            UPDATE workflow_runs
            SET lease_expires_at =
                clock_timestamp() - INTERVAL '1 second'
            WHERE id = :id
            """)
                .param("id", workflow.id())
                .update();

        var newLease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow();

        assertTrue(
                newLease.fencingToken()
                        > oldLease.fencingToken()
        );

        // Old executor must not commit a late result.
        assertThrows(
                IllegalStateException.class,
                () -> workflows.completeStepWithOutput(
                        step.id(),
                        AgentRole.REVIEWER,
                        "Late reviewer acceptance",
                        java.util.Map.of(
                                "decision", "ACCEPT"
                        ),
                        null,
                        oldLease
                )
        );

        assertTrue(
                workflows.output(step.id()).isEmpty()
        );

        assertEquals(
                WorkflowStepStatus.RUNNING,
                workflows.getStep(step.id()).status()
        );

        // A new owner must not bypass an already-running step.
        assertThrows(
                IllegalStateException.class,
                () -> workflows.beginStep(
                        workflow.id(),
                        AgentRole.INVESTIGATOR,
                        newLease
                )
        );

        assertFalse(leases.isOwner(oldLease));
        assertTrue(leases.isOwner(newLease));
    }

}
