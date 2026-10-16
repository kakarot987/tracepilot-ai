
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.HistoricalIncident;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Repository
@Profile("simulator")
public class InMemoryIncidentHistoryRepository
        implements IncidentHistoryRepository {

    private static final List<HistoricalIncident> HISTORY = List.of(
            new HistoricalIncident(
                    "HIST-PAY-001",
                    "INC-HIST-PAY-001",
                    "payment-service",
                    Instant.parse("2026-09-20T09:10:00Z"),
                    "Intermittent payment latency",
                    "Payment requests experienced elevated latency "
                            + "during a short traffic spike."
            ),
            new HistoricalIncident(
                    "HIST-PAY-002",
                    "INC-HIST-PAY-002",
                    "payment-service",
                    Instant.parse("2026-09-27T11:30:00Z"),
                    "Payment timeout alerts",
                    "Several payment requests returned HTTP 504."
            ),
            new HistoricalIncident(
                    "HIST-DB-001",
                    "INC-HIST-DB-001",
                    "order-service",
                    Instant.parse("2026-09-18T08:00:00Z"),
                    "Order creation errors",
                    "Order requests intermittently failed while "
                            + "waiting for a database connection."
            ),
            new HistoricalIncident(
                    "HIST-UP-001",
                    "INC-HIST-UP-001",
                    "checkout-service",
                    Instant.parse("2026-09-15T14:30:00Z"),
                    "Checkout dependency latency",
                    "Checkout requests showed elevated upstream "
                            + "dependency response times."
            ),
            new HistoricalIncident(
                    "HIST-RATE-001",
                    "INC-HIST-RATE-001",
                    "api-gateway",
                    Instant.parse("2026-09-19T13:00:00Z"),
                    "Gateway response anomalies",
                    "Clients reported increased HTTP 429 responses."
            ),
            new HistoricalIncident(
                    "HIST-MEM-001",
                    "INC-HIST-MEM-001",
                    "inventory-service",
                    Instant.parse("2026-09-21T15:00:00Z"),
                    "Inventory restarts",
                    "Inventory requests slowed before service restarts."
            )
    );

    @Override
    public List<HistoricalIncident> findHistory(
            String serviceName,
            Instant before,
            int limit) {

        if (serviceName == null || serviceName.isBlank()
                || before == null || limit < 1 || limit > 20) {
            throw new IllegalArgumentException(
                    "Invalid history query"
            );
        }

        return HISTORY.stream()
                .filter(h -> h.serviceName().equals(serviceName))
                .filter(h -> h.occurredAt().isBefore(before))
                .sorted(
                        Comparator.comparing(
                                        HistoricalIncident::occurredAt
                                ).reversed()
                                .thenComparing(HistoricalIncident::evidenceId)
                )
                .limit(limit)
                .toList();
    }
}
