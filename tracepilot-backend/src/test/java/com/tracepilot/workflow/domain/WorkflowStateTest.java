
package com.tracepilot.workflow.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStateTest {

    @Test
    void terminalStatusesAreCorrect() {
        assertTrue(WorkflowStatus.COMPLETED.isTerminal());
        assertTrue(
                WorkflowStatus.INSUFFICIENT_EVIDENCE.isTerminal()
        );
        assertTrue(WorkflowStatus.TIMED_OUT.isTerminal());

        assertFalse(WorkflowStatus.PENDING.isTerminal());
        assertFalse(WorkflowStatus.RUNNING.isTerminal());
    }

    @Test
    void rejectsNegativeCounters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkflowState(
                        UUID.randomUUID(),
                        "INC-DB-001",
                        WorkflowStatus.RUNNING,
                        AgentRole.TRIAGE,
                        List.of(),
                        List.of(),
                        List.of(),
                        -1,
                        0,
                        Instant.now()
                )
        );
    }

    @Test
    void copiesMutableCollections() {
        var evidence = new ArrayList<String>();
        evidence.add("LOG-DB-001");

        var state = new WorkflowState(
                UUID.randomUUID(),
                "INC-DB-001",
                WorkflowStatus.RUNNING,
                AgentRole.INVESTIGATOR,
                evidence,
                List.of(),
                List.of(),
                1,
                0,
                Instant.now()
        );

        evidence.add("LOG-DB-002");

        assertEquals(
                List.of("LOG-DB-001"),
                state.evidenceIds()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> state.evidenceIds().add("LOG-DB-003")
        );
    }
}
