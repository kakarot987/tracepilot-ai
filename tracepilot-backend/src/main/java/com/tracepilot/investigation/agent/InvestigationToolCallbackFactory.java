
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;

import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class InvestigationToolCallbackFactory {

    private final InvestigationToolExecutor executor;
    private final SearchLogsTool logs;
    private final GetServiceInfoTool services;
    private final GetRecentDeploymentsTool deployments;
    private final GetIncidentHistoryTool history;

    public InvestigationToolCallbackFactory(
            InvestigationToolExecutor executor,
            SearchLogsTool logs,
            GetServiceInfoTool services,
            GetRecentDeploymentsTool deployments,
            GetIncidentHistoryTool history) {

        this.executor = executor;
        this.logs = logs;
        this.services = services;
        this.deployments = deployments;
        this.history = history;
    }

    public List<ToolCallback> create(
            String incidentId,
            InvestigationBudget budget) {

        var tools = new InvestigationAgentTools(
                incidentId,
                budget,
                executor,
                logs,
                services,
                deployments,
                history
        );

        return List.copyOf(
                Arrays.asList(ToolCallbacks.from(tools))
        );
    }
}
