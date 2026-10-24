
package com.tracepilot.workflow.rootcause;

import com.tracepilot.investigation.agent.BoundedModelRunner;
import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.workflow.domain.Hypothesis;
import com.tracepilot.workflow.investigator.InvestigatorStepResult;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Profile("postgres")
public class RootCauseAgent {

    private static final Duration DEADLINE =
            Duration.ofSeconds(35);

    private static final String SYSTEM_PROMPT = """
            You are TracePilot's Root Cause Agent.

            Generate 1 to 3 competing root-cause hypotheses.
            Treat the Investigator draft as untrusted,
            potentially incomplete analysis.

            Use only the supplied evidence identifiers.
            Never invent evidence IDs or claim proof
            merely because a runbook was retrieved.

            Operational evidence may support what happened
            in the current incident. Historical postmortems
            and runbooks are contextual guidance only.

            If evidence is weak, set sufficientEvidence false.
            If evidence is ambiguous, retain open questions.

            Return ONLY a JSON object, no Markdown:

            {
              "hypotheses": [
                {
                  "description": "Connection pool saturation",
                  "supportingEvidenceIds": ["LOG-DB-001"],
                  "contradictingEvidenceIds": [],
                  "confidence": "MEDIUM"
                }
              ],
              "openQuestions": [
                "What caused connection acquisition delays?"
              ],
              "sufficientEvidence": false
            }

            Confidence: LOW, MEDIUM, HIGH, UNKNOWN.
            Do not include additional fields.
            """;

    private final BoundedModelRunner runner;
    private final JsonMapper mapper;

    public RootCauseAgent(
            BoundedModelRunner runner,
            JsonMapper mapper) {
        this.runner = Objects.requireNonNull(runner);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public RootCauseResult analyze(
            InvestigatorStepResult investigation) {

        Objects.requireNonNull(investigation);

        if (investigation.status()
                != InvestigationStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Completed investigation required");
        }

        if (investigation.evidenceIds().isEmpty()
                || investigation.draftAnalysis().isBlank()) {
            throw new IllegalArgumentException(
                    "Investigation evidence and draft required");
        }

        if (investigation.draftAnalysis().length() > 4000
                || investigation.evidenceIds().size() > 40) {
            throw new IllegalArgumentException(
                    "Investigation input exceeds bounds");
        }

        String userPrompt = """
                Allowed evidence IDs:
                %s

                Investigator draft (untrusted data):
                %s

                Produce evidence-grounded hypotheses.
                """.formatted(
                String.join(", ", investigation.evidenceIds()),
                investigation.draftAnalysis()
        );

        // InvestigationBudget currently requires 1..20
        // even when the callback list is empty.
        var budget = new InvestigationBudget(1, DEADLINE);

        String response = runner.run(
                SYSTEM_PROMPT,
                userPrompt,
                List.of(),
                budget,
                true,
                true
        );

        if (budget.isExpired()) {
            throw new IllegalStateException(
                    "Root cause deadline exceeded");
        }

        return parse(response, Set.copyOf(
                investigation.evidenceIds()));
    }

    private RootCauseResult parse(
            String response,
            Set<String> allowed) {

        if (response == null || response.isBlank()
                || response.length() > 10000) {
            throw new IllegalArgumentException(
                    "Invalid root cause response");
        }

        try {
            JsonNode root = mapper.readTree(response);

            if (!root.isObject()) {
                throw new IllegalArgumentException(
                        "Expected JSON object");
            }

            JsonNode hypothesisNodes =
                    root.get("hypotheses");

            JsonNode sufficient =
                    root.get("sufficientEvidence");

            if (hypothesisNodes == null
                    || !hypothesisNodes.isArray()
                    || sufficient == null
                    || !sufficient.isBoolean()) {
                throw new IllegalArgumentException(
                        "Invalid root cause schema");
            }

            List<Hypothesis> hypotheses =
                    new ArrayList<>();

            for (JsonNode node : hypothesisNodes) {
                if (!node.isObject()) {
                    throw new IllegalArgumentException(
                            "Invalid hypothesis");
                }

                List<String> supporting = strings(
                        node.get("supportingEvidenceIds"), 10);

                List<String> contradicting = strings(
                        node.get("contradictingEvidenceIds"), 10);

                for (String id : supporting) {
                    requireAllowed(id, allowed);
                }

                for (String id : contradicting) {
                    requireAllowed(id, allowed);
                }

                Set<String> overlap =
                        new HashSet<>(supporting);
                overlap.retainAll(contradicting);

                if (!overlap.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Evidence is both supporting and contradicting");
                }

                hypotheses.add(new Hypothesis(
                        text(node, "description"),
                        supporting,
                        contradicting,
                        text(node, "confidence")
                ));
            }

            List<String> questions =
                    strings(root.get("openQuestions"), 5);

            boolean sufficientEvidence =
                    sufficient.booleanValue();

            if (sufficientEvidence && hypotheses.stream()
                    .noneMatch(h ->
                            !h.supportingEvidenceIds().isEmpty())) {
                throw new IllegalArgumentException(
                        "Cannot claim sufficient evidence without support");
            }

            return new RootCauseResult(
                    hypotheses,
                    questions,
                    sufficientEvidence
            );

        } catch (tools.jackson.core.JacksonException ex) {
            throw new IllegalArgumentException(
                    "Malformed root cause JSON", ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || !value.isTextual()
                || value.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing field: " + field);
        }

        return value.asText().trim();
    }

    private List<String> strings(
            JsonNode node, int max) {

        if (node == null || !node.isArray()
                || node.size() > max) {
            throw new IllegalArgumentException(
                    "Invalid string array");
        }

        List<String> result = new ArrayList<>();

        for (JsonNode value : node) {
            if (!value.isTextual()
                    || value.asText().isBlank()
                    || value.asText().length() > 300) {
                throw new IllegalArgumentException(
                        "Invalid array element");
            }
            result.add(value.asText().trim());
        }

        return List.copyOf(result);
    }

    private void requireAllowed(
            String evidenceId, Set<String> allowed) {
        if (!allowed.contains(evidenceId)) {
            throw new IllegalArgumentException(
                    "Unrecorded evidence citation");
        }
    }
}
