
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.telemetry.domain.LogEvidence;
import com.tracepilot.telemetry.domain.DeploymentEvidence;
import com.tracepilot.incident.domain.HistoricalIncident;
import com.tracepilot.incident.domain.ServiceInfo;

import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.common.ToolResult;
import com.tracepilot.tools.logs.SearchLogsRequest;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsRequest;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryRequest;
import com.tracepilot.tools.history.GetIncidentHistoryTool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class InvestigationAgentTools {

    private final String incidentId;
    private final InvestigationBudget budget;
    private final InvestigationToolExecutor executor;

    private final SearchLogsTool logs;
    private final GetServiceInfoTool services;
    private final GetRecentDeploymentsTool deployments;
    private final GetIncidentHistoryTool history;

    public InvestigationAgentTools(
            String incidentId,
            InvestigationBudget budget,
            InvestigationToolExecutor executor,
            SearchLogsTool logs,
            GetServiceInfoTool services,
            GetRecentDeploymentsTool deployments,
            GetIncidentHistoryTool history) {

        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException(
                    "incidentId is required"
            );
        }

        this.incidentId = incidentId;
        this.budget = Objects.requireNonNull(budget);
        this.executor = Objects.requireNonNull(executor);
        this.logs = Objects.requireNonNull(logs);
        this.services = Objects.requireNonNull(services);
        this.deployments = Objects.requireNonNull(deployments);
        this.history = Objects.requireNonNull(history);
    }

    @Tool(
            name = "searchLogs",
            description = "Retrieve logs for the current incident. "
                    + "Time values must be ISO-8601 UTC instants. "
                    + "Maximum window is 24 hours and limit is 50."
    )
    public ToolResult<LogEvidence> searchLogs(
            @ToolParam(description = "Start UTC timestamp")
            String from,
            @ToolParam(description = "End UTC timestamp")
            String to,
            @ToolParam(description = "Maximum rows, 1 to 50")
            Integer limit) {

        return executor.execute(budget, () ->
                logs.search(new SearchLogsRequest(
                        incidentId,
                        Instant.parse(from),
                        Instant.parse(to),
                        limit
                ))
        );
    }

    @Tool(
            name = "getServiceInfo",
            description = "Read service metadata for the current incident."
    )
    public ToolResult<ServiceInfo> getServiceInfo() {
        return executor.execute(budget, () ->
                services.get(incidentId).stream().toList()
        );
    }

    @Tool(
            name = "getRecentDeployments",
            description = "Retrieve deployments for the current "
                    + "incident's service. Time values are ISO-8601 "
                    + "UTC instants. Maximum window is seven days "
                    + "and limit is 20."
    )
    public ToolResult<DeploymentEvidence> getRecentDeployments(
            @ToolParam(description = "Start UTC timestamp")
            String from,
            @ToolParam(description = "End UTC timestamp")
            String to,
            @ToolParam(description = "Maximum rows, 1 to 20")
            Integer limit) {

        return executor.execute(budget, () ->
                deployments.get(
                        new GetRecentDeploymentsRequest(
                                incidentId,
                                Instant.parse(from),
                                Instant.parse(to),
                                limit
                        )
                )
        );
    }

    @Tool(
            name = "getIncidentHistory",
            description = "Retrieve earlier incidents for the "
                    + "current incident's service. Maximum limit is 10."
    )
    public ToolResult<HistoricalIncident> getIncidentHistory(
            @ToolParam(description = "Maximum rows, 1 to 10")
            Integer limit) {

        return executor.execute(budget, () ->
                history.get(
                        new GetIncidentHistoryRequest(
                                incidentId,
                                limit
                        )
                )
        );
    }
}
