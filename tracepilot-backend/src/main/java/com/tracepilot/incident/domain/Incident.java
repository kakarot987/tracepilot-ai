
package com.tracepilot.incident.domain;

import java.time.Instant;
import java.util.Objects;

public record Incident(
        String id,
        String title,
        String description,
        String serviceName,
        Instant occurredAt
) {
    public Incident {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(occurredAt);

        if (id.isBlank() || title.isBlank()
                || serviceName.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident identifiers and names must not be blank"
            );
        }
    }
}
