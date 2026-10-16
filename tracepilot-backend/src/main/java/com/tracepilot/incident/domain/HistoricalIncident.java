
package com.tracepilot.incident.domain;

import java.time.Instant;
import java.util.Objects;

public record HistoricalIncident(
        String evidenceId,
        String incidentId,
        String serviceName,
        Instant occurredAt,
        String title,
        String summary
) {
    public HistoricalIncident {
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(incidentId);
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(title);
        Objects.requireNonNull(summary);

        if (evidenceId.isBlank() || incidentId.isBlank()
                || serviceName.isBlank()) {
            throw new IllegalArgumentException(
                    "Historical incident identifiers cannot be blank"
            );
        }
    }
}
