
package com.tracepilot.tools;

import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.logs.SearchLogsRequest;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsRequest;
import com.tracepilot.tools.history.GetIncidentHistoryTool;
import com.tracepilot.tools.history.GetIncidentHistoryRequest;

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
class PostgresToolWiringIntegrationTest {

    @Autowired
    private SearchLogsTool logs;

    @Autowired
    private GetServiceInfoTool services;

    @Autowired
    private GetRecentDeploymentsTool deployments;

    @Autowired
    private GetIncidentHistoryTool history;

    @Test
    void allFourToolsRetrievePersistedEvidence() {
        var logResults = logs.search(new SearchLogsRequest(
                "INC-PAY-001",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:01:00Z"),
                10
        ));

        assertEquals(3, logResults.size());

        var info = services.get("INC-PAY-001").orElseThrow();
        assertEquals("payment-service", info.serviceName());

        var deploymentResults = deployments.get(
                new GetRecentDeploymentsRequest(
                        "INC-PAY-001",
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-02T00:00:00Z"),
                        10
                )
        );

        assertEquals(1, deploymentResults.size());

        var historyResults = history.get(
                new GetIncidentHistoryRequest(
                        "INC-PAY-001", 10
                )
        );

        assertEquals(2, historyResults.size());
    }
}
