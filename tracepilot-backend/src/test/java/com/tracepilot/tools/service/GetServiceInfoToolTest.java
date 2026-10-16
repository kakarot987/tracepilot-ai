
package com.tracepilot.tools.service;

import com.tracepilot.incident.repository.InMemoryIncidentRepository;
import com.tracepilot.incident.repository.InMemoryServiceInfoRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetServiceInfoToolTest {

    private final GetServiceInfoTool tool =
            new GetServiceInfoTool(
                    new InMemoryIncidentRepository(),
                    new InMemoryServiceInfoRepository()
            );

    @Test
    void returnsMetadataForKnownIncident() {
        var info = tool.get("INC-PAY-001").orElseThrow();

        assertEquals("payment-service", info.serviceName());
        assertEquals("Payments", info.team());
        assertEquals("payments-db", info.databaseDependency());
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        assertTrue(tool.get("INC-UNKNOWN").isEmpty());
    }

    @Test
    void rejectsBlankIncidentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tool.get(" ")
        );
    }

    @Test
    void rejectsOversizedIncidentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tool.get("X".repeat(101))
        );
    }

    @Test
    void rejectsNullIncidentId() {
        assertThrows(
                NullPointerException.class,
                () -> tool.get(null)
        );
    }
}
