
package com.tracepilot.incident.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.config.additional-location=optional:file:./config/"
})
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class JdbcIncidentRepositoryIntegrationTest {

    @Autowired
    private IncidentRepository repository;

    @Test
    void loadsSeededIncidents() {
        var incidents = repository.findAll();

        assertEquals(5, incidents.size());

        var payment = repository.findById("INC-PAY-001")
                .orElseThrow();

        assertEquals(
                "payment-service",
                payment.serviceName()
        );
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        assertTrue(
                repository.findById("INC-UNKNOWN").isEmpty()
        );
    }
}
