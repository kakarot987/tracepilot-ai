
package com.tracepilot.telemetry.domain;

import java.time.Instant;
import java.util.Objects;

public record LogEvidence(
        String evidenceId,
        String incidentId,
        String serviceName,
        Instant timestamp,
        String level,
        String message
) {
    public LogEvidence {
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(incidentId);
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(level);
        Objects.requireNonNull(message);

        if (evidenceId.isBlank()
                || incidentId.isBlank()
                || serviceName.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence identifiers must not be blank"
            );
        }
    }
}
