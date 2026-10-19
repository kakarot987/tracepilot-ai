
package com.tracepilot.workflow.domain;

import java.util.List;

public record Hypothesis(
        String description,
        List<String> supportingEvidenceIds,
        List<String> contradictingEvidenceIds,
        String confidence
) {
    public Hypothesis {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Hypothesis description is required"
            );
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
    }
}
