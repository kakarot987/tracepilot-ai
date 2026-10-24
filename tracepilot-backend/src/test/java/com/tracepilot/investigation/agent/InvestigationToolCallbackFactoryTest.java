
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.rag.search.RunbookSearchService;
import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationToolCallbackFactoryTest {

    @Test
    void registersExactlyFiveReadOnlyTools() {
        var factory = new InvestigationToolCallbackFactory(
                new InvestigationToolExecutor(),
                mock(SearchLogsTool.class),
                mock(GetServiceInfoTool.class),
                mock(GetRecentDeploymentsTool.class),
                mock(GetIncidentHistoryTool.class),
                mock(RunbookSearchService.class)
        );

        var callbacks = factory.create(
                "INC-PAY-001",
                new InvestigationBudget(
                        8, Duration.ofMinutes(1)
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
                "getIncidentHistory",
                "searchRunbooks"
        ), names);

        assertEquals(5, callbacks.size());
    }

    @Test
    void registersExactlyFiveReadOnlyToolsUniqueName() {
        var factory = new InvestigationToolCallbackFactory(
                new InvestigationToolExecutor(),
                mock(SearchLogsTool.class),
                mock(GetServiceInfoTool.class),
                mock(GetRecentDeploymentsTool.class),
                mock(GetIncidentHistoryTool.class),
                mock(RunbookSearchService.class)
        );

        var callbacks = factory.create(
                "INC-PAY-001",
                new InvestigationBudget(
                        8, Duration.ofMinutes(1)
                )
        );

        var definitions = callbacks.stream()
                .map(ToolCallback::getToolDefinition)
                .toList();

        assertEquals(5, definitions.size());

        assertEquals(
                5,
                definitions.stream()
                        .map(definition -> definition.name())
                        .distinct()
                        .count()
        );

        assertTrue(
                definitions.stream().allMatch(
                        definition -> definition.inputSchema() != null
                                && !definition.inputSchema().isBlank()
                )
        );
    }
}
