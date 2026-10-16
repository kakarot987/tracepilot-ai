
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
            ),
            new LogEvidence(
                    "LOG-DB-001", "INC-DB-001", "order-service",
                    Instant.parse("2026-10-02T11:00:00Z"),
                    "INFO", "Order request received"
            ),
            new LogEvidence(
                    "LOG-DB-002", "INC-DB-001", "order-service",
                    Instant.parse("2026-10-02T11:00:03Z"),
                    "WARN", "Database connection acquisition delayed"
            ),
            new LogEvidence(
                    "LOG-DB-003", "INC-DB-001", "order-service",
                    Instant.parse("2026-10-02T11:00:08Z"),
                    "ERROR", "Unable to acquire JDBC connection within 3000ms"
            ),

            new LogEvidence(
                    "LOG-UP-001", "INC-UP-001", "checkout-service",
                    Instant.parse("2026-10-03T12:00:00Z"),
                    "INFO", "Checkout request received"
            ),
            new LogEvidence(
                    "LOG-UP-002", "INC-UP-001", "checkout-service",
                    Instant.parse("2026-10-03T12:00:02Z"),
                    "WARN", "External pricing service response delayed"
            ),
            new LogEvidence(
                    "LOG-UP-003", "INC-UP-001", "checkout-service",
                    Instant.parse("2026-10-03T12:00:05Z"),
                    "ERROR", "Upstream pricing request failed with HTTP 503"
            ),

            new LogEvidence(
                    "LOG-RATE-001", "INC-RATE-001", "api-gateway",
                    Instant.parse("2026-10-04T13:00:00Z"),
                    "INFO", "Incoming request rate increased"
            ),
            new LogEvidence(
                    "LOG-RATE-002", "INC-RATE-001", "api-gateway",
                    Instant.parse("2026-10-04T13:00:02Z"),
                    "WARN", "Rate limit threshold exceeded for client group"
            ),
            new LogEvidence(
                    "LOG-RATE-003", "INC-RATE-001", "api-gateway",
                    Instant.parse("2026-10-04T13:00:04Z"),
                    "ERROR", "Request rejected with HTTP 429"
            ),

            new LogEvidence(
                    "LOG-MEM-001", "INC-MEM-001", "inventory-service",
                    Instant.parse("2026-10-05T14:00:00Z"),
                    "INFO", "Inventory synchronization started"
            ),
            new LogEvidence(
                    "LOG-MEM-002", "INC-MEM-001", "inventory-service",
                    Instant.parse("2026-10-05T14:00:03Z"),
                    "WARN", "JVM heap usage above 90 percent"
            ),
            new LogEvidence(
                    "LOG-MEM-003", "INC-MEM-001", "inventory-service",
                    Instant.parse("2026-10-05T14:00:07Z"),
                    "ERROR", "Java heap space allocation failed"
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
