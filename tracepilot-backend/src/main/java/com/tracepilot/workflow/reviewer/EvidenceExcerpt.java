
package com.tracepilot.workflow.reviewer;

import java.util.Objects;

public record EvidenceExcerpt(
        String evidenceId,
        String evidenceType,
        String content
) {
    public EvidenceExcerpt {
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(evidenceType);
        Objects.requireNonNull(content);

        if (evidenceId.isBlank()
                || evidenceType.isBlank()
                || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence fields cannot be blank");
        }

        if (content.length() > 1200) {
            throw new IllegalArgumentException(
                    "Evidence excerpt exceeds 1200 characters");
        }
    }

    public boolean isHistorical() {
        return evidenceType.equals("RUNBOOK_CHUNK")
                || evidenceType.equals("POSTMORTEM_CHUNK");
    }
}
