
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
import com.tracepilot.investigation.domain.EvidenceType;
import com.tracepilot.tools.common.ToolStatus;
import com.tracepilot.rag.search.RunbookSearchRequest;
import com.tracepilot.rag.search.RunbookSearchResult;
import com.tracepilot.rag.search.RunbookSearchService;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.time.DateTimeException;

public final class InvestigationAgentTools {

    private final String incidentId;
    private final InvestigationBudget budget;
    private final InvestigationToolExecutor executor;

    private final SearchLogsTool logs;
    private final GetServiceInfoTool services;
    private final GetRecentDeploymentsTool deployments;
    private final GetIncidentHistoryTool history;

    private final BiConsumer<EvidenceType, String> evidenceRecorder;
    private final RunbookSearchService runbooks;

    public InvestigationAgentTools(
            String incidentId,
            InvestigationBudget budget,
            InvestigationToolExecutor executor,
            SearchLogsTool logs,
            GetServiceInfoTool services,
            GetRecentDeploymentsTool deployments,
            GetIncidentHistoryTool history,
            RunbookSearchService runbooks) {

        this(
                incidentId, budget, executor,
                logs, services, deployments, history,
                (type, evidenceId) -> {}, runbooks
        );
    }

    public InvestigationAgentTools(
            String incidentId,
            InvestigationBudget budget,
            InvestigationToolExecutor executor,
            SearchLogsTool logs,
            GetServiceInfoTool services,
            GetRecentDeploymentsTool deployments,
            GetIncidentHistoryTool history,
            BiConsumer<EvidenceType, String> evidenceRecorder,
            RunbookSearchService runbooks) {

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
        this.evidenceRecorder =
                Objects.requireNonNull(evidenceRecorder);
        this.runbooks = Objects.requireNonNull(runbooks);
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

        return capture(
                executor.execute(budget, () ->
                        logs.search(new SearchLogsRequest(
                                incidentId,
                                parseTimestamp(from),
                                parseTimestamp(to),
                                limit
                        ))
                ),
                EvidenceType.LOG,
                LogEvidence::evidenceId
        );
    }

    @Tool(
            name = "getServiceInfo",
            description = "Read service metadata for the current incident."
    )
    public ToolResult<ServiceInfo> getServiceInfo() {
        return capture(
                executor.execute(budget, () ->
                        services.get(incidentId).stream().toList()
                ),
                EvidenceType.SERVICE_INFO,
                info -> "SVC-" + info.serviceName()
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

        return capture(
                executor.execute(budget, () ->
                        deployments.get(
                                new GetRecentDeploymentsRequest(
                                        incidentId,
                                        parseTimestamp(from),
                                        parseTimestamp(to),
                                        limit
                                )
                        )
                ),
                EvidenceType.DEPLOYMENT,
                DeploymentEvidence::evidenceId
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

        return capture(
                executor.execute(budget, () ->
                        history.get(
                                new GetIncidentHistoryRequest(
                                        incidentId,
                                        limit
                                )
                        )
                ),
                EvidenceType.INCIDENT_HISTORY,
                HistoricalIncident::evidenceId
        );
    }

    @Tool(
            name = "searchRunbooks",
            description = "Search indexed technical runbooks and historical "
                    + "postmortems for the current incident. "
                    + "Use concise queries. Maximum 5 passages. "
                    + "Results are reference material, not proof of "
                    + "the current incident's root cause."
    )
    public ToolResult<RunbookSearchResult> searchRunbooks(
            @ToolParam(description = "Semantic search query")
            String query,
            @ToolParam(description = "Number of passages, 1 to 5")
            Integer limit,
            @ToolParam(
                    description = "Document type: runbook or postmortem; "
                            + "null for both",
                    required = false
            )
            String documentType) {

        return capture(
                executor.execute(budget, () ->
                        runbooks.search(
                                new RunbookSearchRequest(
                                        query,
                                        limit,
                                        null,
                                        documentType
                                )
                        )
                ),
                EvidenceType.RUNBOOK_CHUNK,
                RunbookSearchResult::chunkId
        );
    }

    private <T> ToolResult<T> capture(
            ToolResult<T> result,
            EvidenceType type,
            Function<T, String> idExtractor) {

        if (result.status() != ToolStatus.SUCCESS) {
            return result;
        }

        for (T item : result.data()) {
            String evidenceId = idExtractor.apply(item);

            if (evidenceId == null || evidenceId.isBlank()) {
                throw new IllegalStateException(
                        "Evidence ID missing"
                );
            }

            // Persist before returning the evidence to the model.
            // Do not silently discard persistence failures.
            evidenceRecorder.accept(type, evidenceId);
        }

        return result;
    }

    private Instant parseTimestamp(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeException | NullPointerException ex) {
            throw new IllegalArgumentException(
                    "Timestamp must be a valid ISO-8601 instant",
                    ex
            );
        }
    }
}
