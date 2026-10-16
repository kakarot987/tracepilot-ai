
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.Incident;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("simulator")
public class InMemoryIncidentRepository
        implements IncidentRepository {

    private static final List<Incident> INCIDENTS = List.of(
            new Incident(
                    "INC-PAY-001",
                    "Payment request timeouts",
                    "Payment requests intermittently return HTTP 504.",
                    "payment-service",
                    Instant.parse("2026-10-01T10:00:00Z")
            ),
            new Incident(
                    "INC-DB-001",
                    "Order database connection failures",
                    "Order creation requests fail during peak traffic.",
                    "order-service",
                    Instant.parse("2026-10-02T11:00:00Z")
            ),
            new Incident(
                    "INC-UP-001",
                    "Checkout upstream failures",
                    "Checkout API returns intermittent HTTP 502 responses.",
                    "checkout-service",
                    Instant.parse("2026-10-03T12:00:00Z")
            ),
            new Incident(
                    "INC-RATE-001",
                    "API gateway throttling",
                    "Clients report unexpected HTTP 429 responses.",
                    "api-gateway",
                    Instant.parse("2026-10-04T13:00:00Z")
            ),
            new Incident(
                    "INC-MEM-001",
                    "Inventory memory pressure",
                    "Inventory service latency increases before restarts.",
                    "inventory-service",
                    Instant.parse("2026-10-05T14:00:00Z")
            )
    );

    @Override
    public List<Incident> findAll() {
        return INCIDENTS;
    }

    @Override
    public Optional<Incident> findById(String incidentId) {
        if (incidentId == null) {
            return Optional.empty();
        }

        return INCIDENTS.stream()
                .filter(i -> i.id().equals(incidentId))
                .findFirst();
    }
}
