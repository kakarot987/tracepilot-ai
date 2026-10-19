
package com.tracepilot.telemetry.repository;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLogEvidenceRepositoryTest {

    private final InMemoryLogEvidenceRepository repository =
            new InMemoryLogEvidenceRepository();

    @Test
    void returnsDeterministicEvidenceInOrder() {
        var logs = repository.search(
                "INC-PAY-001",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:01:00Z"),
                10
        );

        assertEquals(3, logs.size());
        assertEquals("LOG-PAY-001", logs.get(0).evidenceId());
        assertEquals("LOG-PAY-002", logs.get(1).evidenceId());
        assertEquals("LOG-PAY-003", logs.get(2).evidenceId());
    }

    @Test
    void respectsRowLimit() {
        var logs = repository.search(
                "INC-PAY-001",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:01:00Z"),
                2
        );

        assertEquals(2, logs.size());
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        var logs = repository.search(
                "INC-UNKNOWN",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:01:00Z"),
                10
        );

        assertTrue(logs.isEmpty());
    }

    @Test
    void rejectsInvalidRowLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.search(
                        "INC-PAY-001",
                        Instant.parse("2026-10-01T10:00:00Z"),
                        Instant.parse("2026-10-01T10:01:00Z"),
                        101
                )
        );
    }

    @Test
    void rejectsInvalidTimeWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.search(
                        "INC-PAY-001",
                        Instant.parse("2026-10-01T11:00:00Z"),
                        Instant.parse("2026-10-01T10:00:00Z"),
                        10
                )
        );
    }
}
