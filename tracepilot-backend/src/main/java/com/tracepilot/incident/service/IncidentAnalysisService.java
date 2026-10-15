
package com.tracepilot.incident.service;

import com.tracepilot.incident.dto.*;
import com.tracepilot.llm.IncidentModelGateway;
import com.tracepilot.llm.exception.ModelOutputException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class IncidentAnalysisService {

    private static final Set<String> SEVERITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final Set<String> CATEGORIES =
            Set.of(
                    "DATABASE", "NETWORK", "APPLICATION",
                    "INFRASTRUCTURE", "EXTERNAL_DEPENDENCY",
                    "UNKNOWN"
            );

    private final IncidentModelGateway gateway;

    public IncidentAnalysisService(
            IncidentModelGateway gateway) {
        this.gateway = gateway;
    }

    public IncidentAnalysisResponse analyze(
            IncidentAnalysisRequest request) {

        IncidentAnalysisResponse result =
                gateway.analyze(request);

        if (result == null
                || !SEVERITIES.contains(result.severity())
                || !CATEGORIES.contains(result.category())
                || result.hypotheses() == null
                || result.hypotheses().isEmpty()
                || result.hypotheses().size() > 3
                || !validHypotheses(result.hypotheses())) {

            throw new ModelOutputException(
                    "The model returned an invalid analysis"
            );
        }

        return result;
    }

    private boolean validHypotheses(
            List<IncidentHypothesis> hypotheses) {

        return hypotheses.stream().allMatch(h ->
                h != null
                        && hasText(h.cause())
                        && hasText(h.reasoning())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
