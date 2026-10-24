package com.tracepilot.investigation.agent;

import com.tracepilot.incident.domain.HistoricalIncident;
import com.tracepilot.incident.domain.Incident;
import com.tracepilot.incident.domain.ServiceInfo;
import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.investigation.domain.EvidenceReference;
import com.tracepilot.investigation.domain.EvidenceType;
import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationRun;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;
import com.tracepilot.rag.search.RunbookSearchService;
import com.tracepilot.telemetry.domain.DeploymentEvidence;
import com.tracepilot.telemetry.domain.LogEvidence;
import com.tracepilot.tools.common.InvestigationToolExecutor;
import com.tracepilot.tools.deployments.GetRecentDeploymentsTool;
import com.tracepilot.tools.history.GetIncidentHistoryTool;
import com.tracepilot.tools.logs.SearchLogsRequest;
import com.tracepilot.tools.logs.SearchLogsTool;
import com.tracepilot.tools.service.GetServiceInfoTool;
import com.tracepilot.workflow.orchestration.FollowUpFocus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Uses the real ToolCallbacks.from(...) and InvestigationToolExecutor.
 * Only the underlying read-only data providers, persistence service,
 * and LLM runner are mocked.
 *
 * The adaptive follow-up tests intentionally fail until that policy
 * is implemented. The model-directed test checks wiring, while the
 * separate BoundedModelRunnerTest checks structured tool execution.
 */
class InvestigationAgentRegressionTest {

    private static final String INCIDENT_ID = "INC-DB-001";
    private static final String SERVICE = "order-service";
    private static final Instant OCCURRED =
            Instant.parse("2026-10-02T11:00:00Z");

    private final UUID runId = UUID.randomUUID();
    private final List<EvidenceReference> recorded = new ArrayList<>();

    private IncidentRepository incidents;
    private InvestigationService investigations;
    private BoundedModelRunner runner;
    private SearchLogsTool logs;
    private GetServiceInfoTool services;
    private GetRecentDeploymentsTool deployments;
    private GetIncidentHistoryTool history;
    private InvestigationAgent agent;

    @BeforeEach
    void setUp() {
        incidents = mock(IncidentRepository.class);
        investigations = mock(InvestigationService.class);
        runner = mock(BoundedModelRunner.class);
        logs = mock(SearchLogsTool.class);
        services = mock(GetServiceInfoTool.class);
        deployments = mock(GetRecentDeploymentsTool.class);
        history = mock(GetIncidentHistoryTool.class);

        Incident incident = new Incident(
                INCIDENT_ID,
                "Order database connection failures",
                "Order creation requests fail during peak traffic",
                SERVICE,
                OCCURRED
        );
        when(incidents.findById(INCIDENT_ID))
                .thenReturn(Optional.of(incident));
        when(investigations.start(INCIDENT_ID))
                .thenReturn(new InvestigationRun(
                        runId,
                        INCIDENT_ID,
                        InvestigationStatus.RUNNING,
                        Instant.now(),
                        null,
                        null
                ));
        when(investigations.evidence(runId))
                .thenAnswer(inv -> List.copyOf(recorded));

        doAnswer(inv -> {
            String id = inv.getArgument(1);
            EvidenceType type = inv.getArgument(2);
            recorded.add(new EvidenceReference(
                    runId, id, type, Instant.now()
            ));
            return null;
        }).when(investigations).recordEvidence(
                eq(runId), anyString(), any(EvidenceType.class)
        );

        var realFactory = new InvestigationToolCallbackFactory(
                new InvestigationToolExecutor(),
                logs,
                services,
                deployments,
                history,
                mock(RunbookSearchService.class)
        );

        agent = new InvestigationAgent(
                runner,
                incidents,
                investigations,
                realFactory,
                new EvidenceCitationValidator()
        );

        // A grounded final response, unless overridden by a test.
        when(runner.run(
                anyString(),
                anyString(),
                anyList(),
                any(InvestigationBudget.class),
                eq(true),
                eq(false)
        )).thenReturn(
                "Connection acquisition delayed [LOG-DB-002]. "
                        + "The underlying cause is not yet established."
        );
    }

    private void seedLog() {
        when(logs.search(any(SearchLogsRequest.class)))
                .thenReturn(List.of(new LogEvidence(
                        "LOG-DB-002", INCIDENT_ID, SERVICE,
                        OCCURRED.plusSeconds(3), "WARN",
                        "Database connection acquisition delayed"
                )));
    }

    private void seedService() {
        when(services.get(INCIDENT_ID)).thenReturn(Optional.of(
                new ServiceInfo(
                        SERVICE, "Orders", "simulation",
                        "Java 21", "orders-db", null
                )
        ));
    }

    private void seedDeployment() {
        when(deployments.get(any())).thenReturn(List.of(
                new DeploymentEvidence(
                        "DEP-DB-001", SERVICE, "2.4.0",
                        OCCURRED.minus(Duration.ofMinutes(40)),
                        "SUCCESS", "Released order validation changes"
                )
        ));
    }

    private void seedHistory() {
        when(history.get(any())).thenReturn(List.of(
                new HistoricalIncident(
                        "HIST-DB-001", "INC-DB-OLD", SERVICE,
                        OCCURRED.minus(Duration.ofDays(2)),
                        "Earlier connection delays",
                        "Similar symptom; cause not independently verified"
                )
        ));
    }

    private static ToolCallback callback(
            List<ToolCallback> callbacks, String name) {
        return callbacks.stream()
                .filter(cb -> cb.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing model-visible callback: " + name
                ));
    }

    @Test
    void initialInvestigationCollectsLogs() {
        seedLog();

        AgentExecution result = agent.investigate(INCIDENT_ID, null, 8);

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(1, result.toolCallsUsed());
        assertEquals(List.of("LOG-DB-002"),
                recorded.stream().map(EvidenceReference::evidenceId).toList());
        verify(logs).search(argThat(request ->
                INCIDENT_ID.equals(request.incidentId())
                        && request.from().equals(OCCURRED.minusSeconds(300))
                        && request.to().equals(OCCURRED.plusSeconds(300))
        ));
        verify(investigations).recordEvidence(
                runId, "LOG-DB-002", EvidenceType.LOG
        );
        verify(runner).run(
                anyString(),
                contains("LOG-DB-002"),
                anyList(),
                any(InvestigationBudget.class),
                eq(true),
                eq(false)
        );
    }

    @Test
    void modelCanRequestAdditionalTool() {
        seedLog();
        seedService();
        when(runner.run(
                anyString(), anyString(), anyList(),
                any(InvestigationBudget.class), eq(true), eq(false)
        )).thenAnswer(inv -> {
            List<ToolCallback> available = inv.getArgument(2);
            // Simulate a model/tool manager selecting an exposed callback.
            callback(available, "getServiceInfo").call("{}");
            return "Delayed acquisition [LOG-DB-002]; "
                    + "service dependency [SVC-order-service].";
        });

        AgentExecution result = agent.investigate(INCIDENT_ID, null, 8);

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(2, result.toolCallsUsed());
        assertTrue(recorded.stream().anyMatch(ref ->
                ref.evidenceType() == EvidenceType.SERVICE_INFO
                        && ref.evidenceId().equals("SVC-order-service")));
        verify(services).get(INCIDENT_ID);
    }

    @Test
    void followUpSelectsAdditionalSources() {
        seedLog();
        seedService();
        seedDeployment();
        FollowUpFocus focus = new FollowUpFocus(List.of(
                "What caused connection saturation?",
                "Were recent deployment changes involved?"
        ));

        AgentExecution result = agent.investigate(INCIDENT_ID, focus, 4);

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertTrue(recorded.stream().anyMatch(ref ->
                ref.evidenceId().equals("SVC-order-service")));
        assertTrue(recorded.stream().anyMatch(ref ->
                ref.evidenceId().equals("DEP-DB-001")));
        assertEquals(3, result.toolCallsUsed());
        verify(services).get(INCIDENT_ID);
        verify(deployments).get(argThat(req ->
                INCIDENT_ID.equals(req.incidentId())
                        && req.limit() != null && req.limit() <= 20
                        && !req.from().isBefore(OCCURRED.minus(Duration.ofDays(7)))
                        && !req.to().isAfter(OCCURRED)
        ));
    }

    @Test
    void followUpRespectsFourToolLimit() {
        seedLog();
        seedService();
        seedDeployment();
        seedHistory();
        FollowUpFocus focus = new FollowUpFocus(List.of(
                "Investigate connection saturation, deployment changes, "
                        + "and previous incident history"
        ));

        AgentExecution result = agent.investigate(INCIDENT_ID, focus, 4);

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(4, result.toolCallsUsed());
        verify(logs, times(1)).search(any());
        verify(services, times(1)).get(INCIDENT_ID);
        verify(deployments, times(1)).get(any());
        verify(history, times(1)).get(any());
        // Once deterministic collection consumes the four-call budget,
        // the LLM must not receive executable callbacks for a fifth call.
        verify(runner).run(
                anyString(), anyString(),
                argThat((List<ToolCallback> available) -> available.isEmpty()),
                any(InvestigationBudget.class), eq(true), eq(false)
        );
    }

    @Test
    void unavailableMetricsRemainUnproven() {
        seedLog();
        FollowUpFocus focus = new FollowUpFocus(List.of(
                "Is there a high request rate?"
        ));

        AgentExecution result = agent.investigate(INCIDENT_ID, focus, 4);

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        verify(runner).run(
                anyString(),
                argThat((String prompt) ->
                        prompt.contains("request-rate metrics")
                                && prompt.contains("Do not claim")
                ),
                anyList(), any(InvestigationBudget.class),
                eq(true), eq(false)
        );
        // This asserts the *instruction* only. A semantic claim
        // still needs evidence-aware Reviewer validation.
        assertFalse(result.draftAnalysis().contains("High request rate confirmed"));
    }

    @Test
    void unsupportedCitationsAreRejected() {
        seedLog();
        when(runner.run(
                anyString(), anyString(), anyList(),
                any(InvestigationBudget.class), eq(true), eq(false)
        )).thenReturn("Invented finding [LOG-FAKE-999].");

        AgentExecution result = agent.investigate(INCIDENT_ID, null, 8);

        assertEquals(InvestigationStatus.INSUFFICIENT_EVIDENCE,
                result.status());
        assertEquals("", result.draftAnalysis());
        assertEquals(1, result.toolCallsUsed());
        verify(investigations).finish(
                runId, InvestigationStatus.INSUFFICIENT_EVIDENCE, null
        );
        assertTrue(recorded.stream().noneMatch(ref ->
                ref.evidenceId().equals("LOG-FAKE-999")));
    }
    @Test
    void fallsBackToDeploymentsWhenLogsAndServiceMetadataAreEmpty() {
        seedDeployment();

        AgentExecution result = agent.investigate(INCIDENT_ID, null, 8);

        assertEquals(InvestigationStatus.INSUFFICIENT_EVIDENCE,
                result.status());
        // The default model draft cites a log that was not retrieved.
        // The fallback must still have persisted the deployment evidence.
        assertTrue(recorded.stream().anyMatch(ref ->
                ref.evidenceId().equals("DEP-DB-001")));
        verify(deployments).get(any());
        assertEquals(3, result.toolCallsUsed());
    }

    @Test
    void noEvidenceAcrossAllSourcesAbstainsWithoutInvokingModel() {
        AgentExecution result = agent.investigate(INCIDENT_ID, null, 8);

        assertEquals(InvestigationStatus.INSUFFICIENT_EVIDENCE,
                result.status());
        assertTrue(recorded.isEmpty());
        assertEquals(5, result.toolCallsUsed());
        verifyNoInteractions(runner);
    }

}
