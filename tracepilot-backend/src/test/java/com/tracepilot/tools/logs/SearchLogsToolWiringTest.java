
package com.tracepilot.tools.logs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("simulator")
class SearchLogsToolWiringTest {

    @Autowired
    private SearchLogsTool tool;

    @Test
    void resolvesSimulatorRepositories() {
        var result = tool.search(new SearchLogsRequest(
                "INC-PAY-001",
                java.time.Instant.parse("2026-10-01T10:00:00Z"),
                java.time.Instant.parse("2026-10-01T10:01:00Z"),
                10
        ));

        assertEquals(3, result.size());
    }
}
