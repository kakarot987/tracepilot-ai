
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;

import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;
import com.tracepilot.investigation.domain.EvidenceType;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.tracepilot.rag.search.RunbookSearchService;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Component
@Profile("postgres")
public class InvestigationToolCallbackFactory {

    private final InvestigationToolExecutor executor;
    private final SearchLogsTool logs;
    private final GetServiceInfoTool services;
    private final GetRecentDeploymentsTool deployments;
    private final GetIncidentHistoryTool history;
    private final RunbookSearchService runbooks;

    public InvestigationToolCallbackFactory(
            InvestigationToolExecutor executor,
            SearchLogsTool logs,
            GetServiceInfoTool services,
            GetRecentDeploymentsTool deployments,
            GetIncidentHistoryTool history,
            RunbookSearchService runbooks) {

        this.executor = executor;
        this.logs = logs;
        this.services = services;
        this.deployments = deployments;
        this.history = history;
        this.runbooks = runbooks;
    }


    public List<ToolCallback> create(
            String incidentId,
            InvestigationBudget budget) {

        return create(
                incidentId,
                budget,
                (type, evidenceId) -> {}
        );
    }

    public List<ToolCallback> create(
            String incidentId,
            InvestigationBudget budget,
            BiConsumer<EvidenceType, String> recorder) {

        var runTools = new InvestigationAgentTools(
                incidentId,
                budget,
                executor,
                logs,
                services,
                deployments,
                history,
                recorder,
                runbooks
        );

        return List.copyOf(
                Arrays.asList(ToolCallbacks.from(runTools))
        );
    }
}
