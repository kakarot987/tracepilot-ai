
package com.tracepilot.tools.history;

import com.tracepilot.incident.repository.InMemoryIncidentHistoryRepository;
import com.tracepilot.incident.repository.InMemoryIncidentRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetIncidentHistoryToolTest {

    private final GetIncidentHistoryTool tool =
            new GetIncidentHistoryTool(
                    new InMemoryIncidentRepository(),
                    new InMemoryIncidentHistoryRepository()
            );

    @Test
    void returnsHistoryForKnownIncident() {
        var results = tool.get(
                new GetIncidentHistoryRequest("INC-PAY-001", 10)
        );

        assertEquals(2, results.size());
        assertEquals("HIST-PAY-002", results.get(0).evidenceId());
        assertEquals("HIST-PAY-001", results.get(1).evidenceId());
    }

    @Test
    void respectsRowLimit() {
        var results = tool.get(
                new GetIncidentHistoryRequest("INC-PAY-001", 1)
        );

        assertEquals(1, results.size());
    }

    @Test
    void excludesCurrentIncident() {
        var results = tool.get(
                new GetIncidentHistoryRequest("INC-PAY-001", 10)
        );

        assertTrue(results.stream().noneMatch(
                h -> h.incidentId().equals("INC-PAY-001")
        ));
    }

    @Test
    void returnsEmptyForUnknownIncident() {
        assertTrue(tool.get(
                new GetIncidentHistoryRequest("INC-UNKNOWN", 10)
        ).isEmpty());
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.get(
                        new GetIncidentHistoryRequest("INC-PAY-001", 11)
                )
        );
    }

    @Test
    void rejectsBlankIncidentId() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.get(
                        new GetIncidentHistoryRequest(" ", 5)
                )
        );
    }

    @Test
    void rejectsMissingRequest() {
        assertThrows(NullPointerException.class,
                () -> tool.get(null)
        );
    }
}
