
package com.tracepilot.tools.logs;

import com.tracepilot.incident.repository.InMemoryIncidentRepository;
import com.tracepilot.telemetry.repository.InMemoryLogEvidenceRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SearchLogsToolTest {

    private final SearchLogsTool tool = new SearchLogsTool(
            new InMemoryIncidentRepository(),
            new InMemoryLogEvidenceRepository()
    );

    private static final Instant FROM =
            Instant.parse("2026-10-01T10:00:00Z");

    private static final Instant TO =
            Instant.parse("2026-10-01T10:01:00Z");

    @Test
    void retrievesEvidenceWithStableIds() {
        var result = tool.search(new SearchLogsRequest(
                "INC-PAY-001", FROM, TO, 10
        ));

        assertEquals(3, result.size());
        assertEquals("LOG-PAY-001", result.get(0).evidenceId());
        assertEquals("LOG-PAY-002", result.get(1).evidenceId());
        assertEquals("LOG-PAY-003", result.get(2).evidenceId());
    }

    @Test
    void respectsRowLimit() {
        var result = tool.search(new SearchLogsRequest(
                "INC-PAY-001", FROM, TO, 2
        ));

        assertEquals(2, result.size());
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        var result = tool.search(new SearchLogsRequest(
                "INC-UNKNOWN", FROM, TO, 10
        ));

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsMissingIncidentId() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.search(new SearchLogsRequest(
                        " ", FROM, TO, 10
                ))
        );
    }

    @Test
    void rejectsOversizedWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.search(new SearchLogsRequest(
                        "INC-PAY-001",
                        FROM,
                        FROM.plusSeconds(25 * 60 * 60),
                        10
                ))
        );
    }

    @Test
    void rejectsInvalidRowLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.search(new SearchLogsRequest(
                        "INC-PAY-001", FROM, TO, 51
                ))
        );
    }

    @Test
    void rejectsReversedWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.search(new SearchLogsRequest(
                        "INC-PAY-001", TO, FROM, 10
                ))
        );
    }

    @Test
    void rejectsMissingTimeWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.search(new SearchLogsRequest(
                        "INC-PAY-001", null, TO, 10
                ))
        );
    }
}
