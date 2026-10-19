
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationToolCallbackFactoryTest {

    @Test
    void registersExactlyFourReadOnlyTools() {
        var factory = new InvestigationToolCallbackFactory(
                new InvestigationToolExecutor(),
                mock(SearchLogsTool.class),
                mock(GetServiceInfoTool.class),
                mock(GetRecentDeploymentsTool.class),
                mock(GetIncidentHistoryTool.class)
        );

        var callbacks = factory.create(
                "INC-PAY-001",
                new InvestigationBudget(
                        5, Duration.ofMinutes(1)
                )
        );

        var names = callbacks.stream()
                .map(callback ->
                        callback.getToolDefinition().name()
                )
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "searchLogs",
                "getServiceInfo",
                "getRecentDeployments",
                "getIncidentHistory"
        ), names);

        assertEquals(4, callbacks.size());
    }
}
