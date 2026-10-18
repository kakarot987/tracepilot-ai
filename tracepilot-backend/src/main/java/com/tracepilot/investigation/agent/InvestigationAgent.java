
package com.tracepilot.investigation.agent;

import com.tracepilot.incident.domain.Incident;
import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@Profile("postgres")
public class InvestigationAgent {

    private static final int MAX_TOOL_CALLS = 8;
    private static final Duration MAX_DURATION =
            Duration.ofSeconds(90);

    private final ChatClient chatClient;
    private final IncidentRepository incidents;
    private final InvestigationService investigations;
    private final InvestigationToolCallbackFactory tools;
    private final EvidenceCitationValidator citationValidator;

    public InvestigationAgent(
            ChatClient.Builder chatClientBuilder,
            IncidentRepository incidents,
            InvestigationService investigations,
            InvestigationToolCallbackFactory tools,
            EvidenceCitationValidator citationValidator) {

        this.chatClient = Objects.requireNonNull(
                chatClientBuilder).build();
        this.incidents = Objects.requireNonNull(incidents);
        this.investigations = Objects.requireNonNull(investigations);
        this.tools = Objects.requireNonNull(tools);
        this.citationValidator =
                Objects.requireNonNull(citationValidator);
    }

    public AgentExecution investigate(String incidentId) {

        Incident incident = incidents.findById(incidentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown incident"
                        )
                );

        // Starting the run happens in a separate
        // transaction inside InvestigationService.
        var run = investigations.start(incident.id());

        var budget = new InvestigationBudget(
                MAX_TOOL_CALLS,
                MAX_DURATION
        );

        try {
            List<ToolCallback> callbacks = tools.create(
                    incident.id(),
                    budget,
                    (type, evidenceId) ->
                            investigations.recordEvidence(
                                    run.id(), evidenceId, type
                            )
            );

            String response = chatClient.prompt()
                    .system("""
                            Every factual finding must cite evidence IDs returned by the tools.
                            
                            Use the exact evidence IDs from tool responses:
                            - Logs: LOG-...
                            - Deployments: DEP-...
                            - History: HIST-...
                            - Service metadata: SVC-{serviceName}
                            
                            For service metadata, derive its citation ID by prefixing
                            the returned serviceName with SVC-.
                            
                            Do not create or guess evidence IDs.
                            
                            If no supporting evidence was retrieved, say that
                            the evidence is insufficient.
                            
                            Citations must appear in the final analysis, not merely
                            in an intermediate tool call.
                            """)
                    .user(buildIncidentPrompt(incident))
                    .tools(
                            (Object) callbacks.toArray(
                                    ToolCallback[]::new
                            )
                    )
                    .call()
                    .content();

            if (budget.isExpired()) {
                return finishWithoutReport(
                        run.id(),
                        InvestigationStatus.TIMED_OUT,
                        "Investigation deadline exceeded",
                        budget
                );
            }

            if (response == null || response.isBlank()) {
                return finishWithoutReport(
                        run.id(),
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        null,
                        budget
                );
            }

            /*
             * Step 13 will persist and validate
             * every tool-returned evidence reference.
             *
             * Until that exists, a model answer is
             * not an accepted investigation report.
             */

            var persistedEvidence =
                    investigations.evidence(run.id());

            var validation = citationValidator.validate(
                    response,
                    persistedEvidence
            );

            if (!validation.valid()) {

                investigations.finish(
                        run.id(),
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        null
                );

                return new AgentExecution(
                        run.id(),
                        InvestigationStatus.INSUFFICIENT_EVIDENCE,
                        "",
                        budget.toolCallsUsed()
                );
            }

            investigations.finish(
                    run.id(),
                    InvestigationStatus.COMPLETED,
                    null
            );

            return new AgentExecution(
                    run.id(),
                    InvestigationStatus.COMPLETED,
                    truncate(response, 4000),
                    budget.toolCallsUsed()
            );

        } catch (RuntimeException ex) {

            InvestigationStatus status =
                    budget.isExpired()
                            ? InvestigationStatus.TIMED_OUT
                            : InvestigationStatus.FAILED;

            // Never store an exception stack trace
            // or provider credentials in the database.
            investigations.finish(
                    run.id(),
                    status,
                    "Agent execution failed"
            );

            return new AgentExecution(
                    run.id(),
                    status,
                    "",
                    budget.toolCallsUsed()
            );
        }
    }

    private AgentExecution finishWithoutReport(
            java.util.UUID runId,
            InvestigationStatus status,
            String reason,
            InvestigationBudget budget) {

        investigations.finish(runId, status, reason);

        return new AgentExecution(
                runId,
                status,
                "",
                budget.toolCallsUsed()
        );
    }

    private String buildIncidentPrompt(Incident incident) {
        return """
                Incident ID: %s
                Title: %s
                Service: %s
                Occurred at: %s
                Description: %s

                Investigate using the available tools.
                Start by gathering evidence. Do not
                assume the root cause from the title.
                """
                .formatted(
                        incident.id(),
                        incident.title(),
                        incident.serviceName(),
                        incident.occurredAt(),
                        incident.description()
                );
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
