
package com.tracepilot.workflow.domain;

import java.util.List;
import java.util.Set;

public record Hypothesis(
        String description,
        List<String> supportingEvidenceIds,
        List<String> contradictingEvidenceIds,
        String confidence
) {
    private static final Set<String> CONFIDENCE_VALUES =
            Set.of("LOW", "MEDIUM", "HIGH", "UNKNOWN");

    public Hypothesis {
        if (description == null || description.isBlank()
                || description.length() > 500) {
            throw new IllegalArgumentException(
                    "Invalid hypothesis description");
        }

        supportingEvidenceIds = List.copyOf(
                supportingEvidenceIds == null
                        ? List.of()
                        : supportingEvidenceIds
        );

        contradictingEvidenceIds = List.copyOf(
                contradictingEvidenceIds == null
                        ? List.of()
                        : contradictingEvidenceIds
        );

        if (supportingEvidenceIds.size() > 10
                || contradictingEvidenceIds.size() > 10) {
            throw new IllegalArgumentException(
                    "Too many evidence references");
        }

        if (!CONFIDENCE_VALUES.contains(confidence)) {
            throw new IllegalArgumentException(
                    "Invalid qualitative confidence");
        }

        for (String id : supportingEvidenceIds) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid supporting evidence ID");
            }
        }

        for (String id : contradictingEvidenceIds) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid contradicting evidence ID");
            }
        }
    }
}
