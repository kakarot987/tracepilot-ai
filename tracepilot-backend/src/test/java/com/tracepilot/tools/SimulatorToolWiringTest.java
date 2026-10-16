
package com.tracepilot.tools;

import com.tracepilot.tools.history.GetIncidentHistoryTool;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("simulator")
class SimulatorToolWiringTest {

    @Autowired
    private SearchLogsTool searchLogsTool;

    @Autowired
    private GetServiceInfoTool serviceInfoTool;

    @Autowired
    private GetRecentDeploymentsTool deploymentsTool;

    @Autowired
    private GetIncidentHistoryTool historyTool;

    @Test
    void allReadOnlyToolsAreAvailable() {
        assertNotNull(searchLogsTool);
        assertNotNull(serviceInfoTool);
        assertNotNull(deploymentsTool);
        assertNotNull(historyTool);
    }
}
