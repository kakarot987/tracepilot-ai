
package com.tracepilot.incident.repository;

import com.tracepilot.telemetry.repository.InMemoryLogEvidenceRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticCatalogTest {

    private final InMemoryIncidentRepository incidents =
            new InMemoryIncidentRepository();

    private final InMemoryServiceInfoRepository services =
            new InMemoryServiceInfoRepository();

    private final InMemoryLogEvidenceRepository logs =
            new InMemoryLogEvidenceRepository();

    @Test
    void catalogContainsFiveIncidents() {
        assertEquals(5, incidents.findAll().size());

        assertEquals(
                5,
                incidents.findAll().stream()
                        .map(i -> i.id())
                        .distinct()
                        .count()
        );
    }

    @Test
    void everyIncidentHasServiceMetadataAndEvidence() {
        for (var incident : incidents.findAll()) {
            assertTrue(
                    services.findByServiceName(
                            incident.serviceName()
                    ).isPresent(),
                    "Missing service: " + incident.serviceName()
            );

            var evidence = logs.search(
                    incident.id(),
                    incident.occurredAt(),
                    incident.occurredAt().plus(
                            Duration.ofMinutes(5)
                    ),
                    100
            );

            assertFalse(
                    evidence.isEmpty(),
                    "Missing evidence: " + incident.id()
            );

            assertTrue(
                    evidence.stream().allMatch(
                            log -> log.serviceName()
                                    .equals(incident.serviceName())
                    ),
                    "Evidence references incorrect service"
            );

            assertEquals(
                    evidence.size(),
                    evidence.stream()
                            .map(log -> log.evidenceId())
                            .distinct()
                            .count(),
                    "Duplicate evidence IDs for " + incident.id()
            );
        }
    }

    @Test
    void incidentIdsResolveConsistently() {
        for (var incident : incidents.findAll()) {
            assertEquals(
                    incident,
                    incidents.findById(incident.id()).orElseThrow()
            );
        }
    }

    @Test
    void unknownIncidentHasNoEvidence() {
        assertTrue(
                logs.search(
                        "INC-NOT-FOUND",
                        java.time.Instant.parse(
                                "2026-10-01T00:00:00Z"
                        ),
                        java.time.Instant.parse(
                                "2026-10-06T00:00:00Z"
                        ),
                        100
                ).isEmpty()
        );
    }
}
