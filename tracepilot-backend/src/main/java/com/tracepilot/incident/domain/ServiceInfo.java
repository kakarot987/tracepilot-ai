
package com.tracepilot.incident.domain;

import java.util.Objects;

public record ServiceInfo(
        String serviceName,
        String team,
        String environment,
        String runtime,
        String databaseDependency,
        String upstreamDependency
) {
    public ServiceInfo {
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(team);
        Objects.requireNonNull(environment);
        Objects.requireNonNull(runtime);

        if (serviceName.isBlank()) {
            throw new IllegalArgumentException(
                    "Service name cannot be blank"
            );
        }
    }
}
