
package com.tracepilot.tools.deployments;

import com.tracepilot.incident.repository.InMemoryIncidentRepository;
import com.tracepilot.telemetry.repository.InMemoryDeploymentEvidenceRepository;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GetRecentDeploymentsToolTest {

    private final GetRecentDeploymentsTool tool =
            new GetRecentDeploymentsTool(
                    new InMemoryIncidentRepository(),
                    new InMemoryDeploymentEvidenceRepository()
            );

    private static final Instant FROM =
            Instant.parse("2026-10-01T00:00:00Z");

    private static final Instant TO =
            Instant.parse("2026-10-02T00:00:00Z");

    @Test
    void retrievesDeploymentWithStableEvidenceId() {
        var results = tool.get(
                new GetRecentDeploymentsRequest(
                        "INC-PAY-001", FROM, TO, 10
                )
        );

        assertEquals(1, results.size());
        assertEquals(
                "DEP-PAY-001",
                results.get(0).evidenceId()
        );
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        assertTrue(tool.get(
                new GetRecentDeploymentsRequest(
                        "INC-UNKNOWN", FROM, TO, 10
                )
        ).isEmpty());
    }

    @Test
    void rejectsOversizedTimeWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.get(
                        new GetRecentDeploymentsRequest(
                                "INC-PAY-001",
                                FROM,
                                FROM.plusSeconds(8 * 24 * 3600),
                                10
                        )
                )
        );
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.get(
                        new GetRecentDeploymentsRequest(
                                "INC-PAY-001", FROM, TO, 21
                        )
                )
        );
    }

    @Test
    void returnsEmptyOutsideDeploymentWindow() {
        assertTrue(tool.get(
                new GetRecentDeploymentsRequest(
                        "INC-PAY-001",
                        Instant.parse("2026-10-02T00:00:00Z"),
                        Instant.parse("2026-10-03T00:00:00Z"),
                        10
                )
        ).isEmpty());
    }

    @Test
    void rejectsMissingRequest() {
        assertThrows(
                NullPointerException.class,
                () -> tool.get(null)
        );
    }
}
