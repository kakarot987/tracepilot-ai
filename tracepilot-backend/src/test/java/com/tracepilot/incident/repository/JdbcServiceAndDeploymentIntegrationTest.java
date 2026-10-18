
package com.tracepilot.incident.repository;

import com.tracepilot.telemetry.repository.DeploymentEvidenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.config.additional-location=optional:file:./config/"
})
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class JdbcServiceAndDeploymentIntegrationTest {

    @Autowired
    private ServiceInfoRepository services;

    @Autowired
    private DeploymentEvidenceRepository deployments;

    @Test
    void loadsSeededServiceMetadata() {
        var info = services.findByServiceName(
                "payment-service"
        ).orElseThrow();

        assertEquals("Payments", info.team());
        assertEquals("payments-db", info.databaseDependency());
    }

    @Test
    void returnsEmptyForUnknownService() {
        assertTrue(
                services.findByServiceName("unknown-service").isEmpty()
        );
    }

    @Test
    void loadsDeploymentWithStableEvidenceId() {
        var records = deployments.findByServiceAndWindow(
                "payment-service",
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T00:00:00Z"),
                10
        );

        assertEquals(1, records.size());
        assertEquals("DEP-PAY-001", records.get(0).evidenceId());
    }

    @Test
    void rejectsInvalidDeploymentLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> deployments.findByServiceAndWindow(
                        "payment-service",
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-02T00:00:00Z"),
                        51
                )
        );
    }
}
