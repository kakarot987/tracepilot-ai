
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.LogEvidence;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Profile("simulator")
public class InMemoryLogEvidenceRepository
        implements LogEvidenceRepository {

    private static final List<LogEvidence> LOGS = List.of(
            new LogEvidence(
                    "LOG-PAY-001",
                    "INC-PAY-001",
                    "payment-service",
                    Instant.parse("2026-10-01T10:00:00Z"),
                    "INFO",
                    "Payment request received"
            ),
            new LogEvidence(
                    "LOG-PAY-002",
                    "INC-PAY-001",
                    "payment-service",
                    Instant.parse("2026-10-01T10:00:03Z"),
                    "WARN",
                    "Waiting for database connection"
            ),
            new LogEvidence(
                    "LOG-PAY-003",
                    "INC-PAY-001",
                    "payment-service",
                    Instant.parse("2026-10-01T10:00:08Z"),
                    "ERROR",
                    "Connection pool acquisition timeout"
            )
    );

    @Override
    public List<LogEvidence> search(
            String incidentId,
            Instant from,
            Instant to,
            int limit) {

        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException(
                    "incidentId is required"
            );
        }

        if (from == null || to == null
                || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Invalid time window"
            );
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 100"
            );
        }

        return LOGS.stream()
                .filter(log ->
                        log.incidentId().equals(incidentId))
                .filter(log ->
                        !log.timestamp().isBefore(from)
                                && !log.timestamp().isAfter(to))
                .sorted(
                        java.util.Comparator
                                .comparing(LogEvidence::timestamp)
                                .thenComparing(LogEvidence::evidenceId)
                )
                .limit(limit)
                .toList();
    }
}
