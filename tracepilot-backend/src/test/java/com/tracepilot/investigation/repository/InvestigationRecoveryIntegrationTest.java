
package com.tracepilot.investigation.repository;

import com.tracepilot.investigation.domain.InvestigationStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
@Transactional
class InvestigationRecoveryIntegrationTest {

    @Autowired
    private InvestigationRunRepository runs;

    @Autowired
    private org.springframework.jdbc.core.simple.JdbcClient jdbc;

    private UUID insertRunning(Instant startedAt) {
        UUID id = UUID.randomUUID();

        jdbc.sql("""
                INSERT INTO investigation_runs
                    (id, incident_id, status, started_at)
                VALUES
                    (:id, 'INC-PAY-001', 'RUNNING', :startedAt)
                """)
                .param("id", id)
                .param(
                        "startedAt",
                        OffsetDateTime.ofInstant(
                                startedAt, ZoneOffset.UTC
                        )
                )
                .update();

        return id;
    }

    @Test
    void recoversAbandonedInvestigation() {
        Instant now = Instant.now();
        UUID id = insertRunning(
                now.minusSeconds(300)
        );

        int recovered = runs.expireStaleRuns(
                now.minusSeconds(120)
        );

        var run = runs.findById(id).orElseThrow();

        assertEquals(1, recovered);
        assertEquals(
                InvestigationStatus.TIMED_OUT,
                run.status()
        );
        assertNotNull(run.completedAt());
        assertFalse(
                run.completedAt().isBefore(run.startedAt())
        );
    }

    @Test
    void leavesRecentInvestigationRunning() {
        Instant now = Instant.now();
        UUID id = insertRunning(now);

        int recovered = runs.expireStaleRuns(
                now.minusSeconds(120)
        );

        assertEquals(0, recovered);
        assertEquals(
                InvestigationStatus.RUNNING,
                runs.findById(id).orElseThrow().status()
        );
    }

    @Test
    void recoveryIsIdempotent() {
        Instant now = Instant.now();
        UUID id = insertRunning(
                now.minusSeconds(300)
        );

        Instant cutoff = now.minusSeconds(120);

        assertEquals(1, runs.expireStaleRuns(cutoff));
        assertEquals(0, runs.expireStaleRuns(cutoff));

        assertEquals(
                InvestigationStatus.TIMED_OUT,
                runs.findById(id).orElseThrow().status()
        );
    }
}
