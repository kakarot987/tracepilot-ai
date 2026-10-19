
package com.tracepilot.telemetry.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class JdbcLogEvidenceRepositoryIntegrationTest {

    @Autowired
    private LogEvidenceRepository repository;

    private static final Instant FROM =
            Instant.parse("2026-10-01T10:00:00Z");

    private static final Instant TO =
            Instant.parse("2026-10-01T10:01:00Z");

    @Test
    void retrievesSeededPaymentLogsInOrder() {
        var logs = repository.search(
                "INC-PAY-001", FROM, TO, 10
        );

        assertEquals(3, logs.size());

        assertEquals("LOG-PAY-001", logs.get(0).evidenceId());
        assertEquals("LOG-PAY-002", logs.get(1).evidenceId());
        assertEquals("LOG-PAY-003", logs.get(2).evidenceId());
    }

    @Test
    void respectsRowLimit() {
        var logs = repository.search(
                "INC-PAY-001", FROM, TO, 2
        );

        assertEquals(2, logs.size());
    }

    @Test
    void returnsNoEvidenceForUnknownIncident() {
        assertTrue(repository.search(
                "INC-UNKNOWN", FROM, TO, 10
        ).isEmpty());
    }

    @Test
    void rejectsInvalidRowLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.search(
                        "INC-PAY-001", FROM, TO, 101
                )
        );
    }

    @Test
    void allSeededIncidentsHaveDistinctEvidenceIds() {
        var cases = Set.of(
                "INC-PAY-001",
                "INC-DB-001",
                "INC-UP-001",
                "INC-RATE-001",
                "INC-MEM-001"
        );

        var allIds = new java.util.HashSet<String>();

        for (String incidentId : cases) {
            var logs = repository.search(
                    incidentId,
                    Instant.parse("2026-10-01T00:00:00Z"),
                    Instant.parse("2026-10-06T00:00:00Z"),
                    100
            );

            assertEquals(
                    3, logs.size(),
                    "Unexpected log count for " + incidentId
            );

            for (var log : logs) {
                assertTrue(
                        allIds.add(log.evidenceId()),
                        "Duplicate evidence ID: " + log.evidenceId()
                );
            }
        }

        assertEquals(15, allIds.size());
    }
}
