
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.DeploymentEvidence;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Repository
@Profile("simulator")
public class InMemoryDeploymentEvidenceRepository
        implements DeploymentEvidenceRepository {

    private static final List<DeploymentEvidence> DEPLOYMENTS =
            List.of(
                    new DeploymentEvidence(
                            "DEP-PAY-001",
                            "payment-service",
                            "1.8.2",
                            Instant.parse("2026-10-01T09:30:00Z"),
                            "SUCCESS",
                            "Updated payment request timeout configuration"
                    ),
                    new DeploymentEvidence(
                            "DEP-DB-001",
                            "order-service",
                            "2.4.0",
                            Instant.parse("2026-10-02T10:20:00Z"),
                            "SUCCESS",
                            "Released order validation changes"
                    ),
                    new DeploymentEvidence(
                            "DEP-UP-001",
                            "checkout-service",
                            "3.1.5",
                            Instant.parse("2026-10-03T09:00:00Z"),
                            "SUCCESS",
                            "Updated checkout telemetry"
                    ),
                    new DeploymentEvidence(
                            "DEP-RATE-001",
                            "api-gateway",
                            "4.2.1",
                            Instant.parse("2026-10-04T12:40:00Z"),
                            "SUCCESS",
                            "Adjusted traffic throttling configuration"
                    ),
                    new DeploymentEvidence(
                            "DEP-MEM-001",
                            "inventory-service",
                            "1.6.0",
                            Instant.parse("2026-10-05T12:00:00Z"),
                            "SUCCESS",
                            "Updated inventory synchronization scheduler"
                    )
            );

    @Override
    public List<DeploymentEvidence> findByServiceAndWindow(
            String serviceName,
            Instant from,
            Instant to,
            int limit) {

        if (serviceName == null || serviceName.isBlank()
                || from == null || to == null
                || !from.isBefore(to)
                || limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "Invalid deployment query"
            );
        }

        return DEPLOYMENTS.stream()
                .filter(d -> d.serviceName().equals(serviceName))
                .filter(d -> !d.deployedAt().isBefore(from))
                .filter(d -> !d.deployedAt().isAfter(to))
                .sorted(
                        Comparator.comparing(
                                        DeploymentEvidence::deployedAt
                                ).reversed()
                                .thenComparing(DeploymentEvidence::evidenceId)
                )
                .limit(limit)
                .toList();
    }
}
