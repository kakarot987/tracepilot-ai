
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;
import com.tracepilot.rag.search.RunbookSearchService;
import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.common.ToolResult;
import com.tracepilot.tools.common.ToolStatus;
import com.tracepilot.tools.logs.SearchLogsRequest;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvestigationAgentToolsTest {

    private SearchLogsTool logs;
    private GetServiceInfoTool services;
    private GetRecentDeploymentsTool deployments;
    private GetIncidentHistoryTool history;
    private InvestigationToolExecutor executor;
    private RunbookSearchService runbooks;
    @BeforeEach
    void setUp() {
        logs = mock(SearchLogsTool.class);
        services = mock(GetServiceInfoTool.class);
        deployments = mock(GetRecentDeploymentsTool.class);
        history = mock(GetIncidentHistoryTool.class);
        executor = new InvestigationToolExecutor();
        runbooks = mock(RunbookSearchService.class);
    }

    private InvestigationAgentTools tools(
            InvestigationBudget budget) {

        return new InvestigationAgentTools(
                "INC-PAY-001",
                budget,
                executor,
                logs,
                services,
                deployments,
                history,
                runbooks
        );
    }

    @Test
    void alwaysUsesCurrentIncidentId() {
        when(logs.search(any(SearchLogsRequest.class)))
                .thenReturn(List.of());

        var result = tools(new InvestigationBudget(
                3, Duration.ofMinutes(1)
        )).searchLogs(
                "2026-10-01T10:00:00Z",
                "2026-10-01T10:01:00Z",
                10
        );

        verify(logs).search(argThat(request ->
                "INC-PAY-001".equals(request.incidentId())
        ));

        assertNotNull(result);
    }

    @Test
    void invalidArgumentsConsumeBudget() {
        var budget = new InvestigationBudget(
                1, Duration.ofMinutes(1)
        );

        var tools = tools(budget);

        ToolResult<?> first = tools.searchLogs(
                "invalid-time",
                "2026-10-01T10:01:00Z",
                10
        );

        assertNotNull(first);
        assertEquals(1, budget.toolCallsUsed());

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> tools.getServiceInfo()
        );

        verifyNoInteractions(services);
    }

    @Test
    void emptyServiceResultIsHandled() {
        when(services.get("INC-PAY-001"))
                .thenReturn(Optional.empty());

        var result = tools(new InvestigationBudget(
                2, Duration.ofMinutes(1)
        )).getServiceInfo();

        assertNotNull(result);
        verify(services).get("INC-PAY-001");
    }

    @Test
    void toolBudgetIsSharedAcrossDifferentMethods() {
        when(services.get("INC-PAY-001"))
                .thenReturn(Optional.empty());

        var budget = new InvestigationBudget(
                1, Duration.ofMinutes(1)
        );

        var tools = tools(budget);

        tools.getServiceInfo();

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> tools.getIncidentHistory(5)
        );

        verifyNoInteractions(history);
    }

    @Test
    void recordsOnlySuccessfullyReturnedEvidence() {

        var recorded = new java.util.ArrayList<String>();

        when(logs.search(any(SearchLogsRequest.class)))
                .thenReturn(List.of(
                        new com.tracepilot.telemetry.domain.LogEvidence(
                                "LOG-PAY-001",
                                "INC-PAY-001",
                                "payment-service",
                                java.time.Instant.parse(
                                        "2026-10-01T10:00:00Z"
                                ),
                                "ERROR",
                                "Connection timeout"
                        )
                ));

        var adapter = new InvestigationAgentTools(
                "INC-PAY-001",
                new InvestigationBudget(
                        3, java.time.Duration.ofMinutes(1)
                ),
                executor,
                logs,
                services,
                deployments,
                history,
                (type, id) -> recorded.add(
                        type.name() + ":" + id
                ),
                runbooks
        );

        adapter.searchLogs(
                "2026-10-01T10:00:00Z",
                "2026-10-01T10:01:00Z",
                10
        );

        assertEquals(
                List.of("LOG:LOG-PAY-001"),
                recorded
        );
    }

    @Test
    void rejectsMalformedDeploymentTimestamp() {

        var adapter = new InvestigationAgentTools(
                "INC-PAY-001",
                new InvestigationBudget(
                        3, Duration.ofMinutes(1)
                ),
                executor,
                logs,
                services,
                deployments,
                history,
                runbooks
        );

        var result = adapter.getRecentDeployments(
                "invalid-timestamp",
                "2026-10-01T10:00:00Z",
                10
        );

        assertEquals(
                ToolStatus.INVALID_ARGUMENTS,
                result.status()
        );

        verifyNoInteractions(deployments);
    }

}
