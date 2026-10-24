
package com.tracepilot.workflow.rootcause;

import com.tracepilot.workflow.domain.Hypothesis;

import java.util.List;
import java.util.Objects;

public record RootCauseResult(
        List<Hypothesis> hypotheses,
        List<String> openQuestions,
        boolean sufficientEvidence
) {
    public RootCauseResult {
        Objects.requireNonNull(hypotheses);
        Objects.requireNonNull(openQuestions);

        if (hypotheses.isEmpty() || hypotheses.size() > 3) {
            throw new IllegalArgumentException(
                    "Expected 1 to 3 hypotheses");
        }

        if (openQuestions.size() > 5) {
            throw new IllegalArgumentException(
                    "At most 5 open questions allowed");
        }

        hypotheses = List.copyOf(hypotheses);
        openQuestions = List.copyOf(openQuestions);
    }
}
