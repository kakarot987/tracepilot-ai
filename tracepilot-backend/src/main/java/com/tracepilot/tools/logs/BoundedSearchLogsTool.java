
package com.tracepilot.tools.logs;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.telemetry.domain.LogEvidence;
import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.common.ToolResult;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("simulator")
public class BoundedSearchLogsTool {

    private final SearchLogsTool searchLogsTool;
    private final InvestigationToolExecutor executor;

    public BoundedSearchLogsTool(
            SearchLogsTool searchLogsTool,
            InvestigationToolExecutor executor) {
        this.searchLogsTool = searchLogsTool;
        this.executor = executor;
    }

    public ToolResult<LogEvidence> search(
            InvestigationBudget budget,
            SearchLogsRequest request) {

        return executor.execute(
                budget,
                () -> searchLogsTool.search(request)
        );
    }
}
