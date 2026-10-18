
package com.tracepilot.incident.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class JdbcIncidentHistoryRepositoryIntegrationTest {

    @Autowired
    private IncidentHistoryRepository history;

    private static final Instant CUTOFF =
            Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void retrievesPaymentHistoryNewestFirst() {
        var results = history.findHistory(
                "payment-service", CUTOFF, 10
        );

        assertEquals(2, results.size());
        assertEquals("HIST-PAY-002", results.get(0).evidenceId());
        assertEquals("HIST-PAY-001", results.get(1).evidenceId());
    }

    @Test
    void respectsRowLimit() {
        var results = history.findHistory(
                "payment-service", CUTOFF, 1
        );

        assertEquals(1, results.size());
    }

    @Test
    void excludesFutureHistory() {
        var results = history.findHistory(
                "payment-service",
                Instant.parse("2026-09-01T00:00:00Z"),
                10
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void returnsEmptyForUnknownService() {
        assertTrue(history.findHistory(
                "unknown-service", CUTOFF, 10
        ).isEmpty());
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> history.findHistory(
                        "payment-service", CUTOFF, 21
                )
        );
    }
}
