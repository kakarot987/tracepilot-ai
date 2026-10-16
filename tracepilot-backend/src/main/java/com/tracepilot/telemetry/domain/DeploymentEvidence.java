
package com.tracepilot.telemetry.domain;

import java.time.Instant;
import java.util.Objects;

public record DeploymentEvidence(
        String evidenceId,
        String serviceName,
        String version,
        Instant deployedAt,
        String status,
        String changeSummary
) {
    public DeploymentEvidence {
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(version);
        Objects.requireNonNull(deployedAt);
        Objects.requireNonNull(status);
        Objects.requireNonNull(changeSummary);

        if (evidenceId.isBlank()
                || serviceName.isBlank()
                || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Deployment identifiers cannot be blank"
            );
        }
    }
}
