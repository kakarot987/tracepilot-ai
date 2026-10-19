
package com.tracepilot.workflow.reviewer;

import java.util.List;

public record ClaimReview(
        int hypothesisIndex,
        boolean supported,
        List<String> evidenceIds,
        String explanation
) {
    public ClaimReview {
        if (hypothesisIndex < 0) {
            throw new IllegalArgumentException(
                    "Invalid hypothesis index");
        }

        evidenceIds = List.copyOf(evidenceIds);

        if (explanation == null
                || explanation.isBlank()
                || explanation.length() > 500) {
            throw new IllegalArgumentException(
                    "Invalid review explanation");
        }
    }
}
