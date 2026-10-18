
package com.tracepilot.tools.deployments;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.telemetry.domain.DeploymentEvidence;
import com.tracepilot.telemetry.repository.DeploymentEvidenceRepository;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class GetRecentDeploymentsTool {

    private static final Duration MAX_WINDOW =
            Duration.ofDays(7);

    private static final int MAX_ROWS = 20;

    private final IncidentRepository incidents;
    private final DeploymentEvidenceRepository deployments;

    public GetRecentDeploymentsTool(
            IncidentRepository incidents,
            DeploymentEvidenceRepository deployments) {
        this.incidents = incidents;
        this.deployments = deployments;
    }

    public List<DeploymentEvidence> get(
            GetRecentDeploymentsRequest request) {

        Objects.requireNonNull(request, "request is required");

        if (request.incidentId() == null
                || request.incidentId().isBlank()
                || request.incidentId().length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid incidentId"
            );
        }

        if (request.from() == null
                || request.to() == null
                || !request.from().isBefore(request.to())) {
            throw new IllegalArgumentException(
                    "Invalid time window"
            );
        }

        if (Duration.between(
                request.from(),
                request.to()
        ).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException(
                    "Deployment query exceeds seven days"
            );
        }

        if (request.limit() == null
                || request.limit() < 1
                || request.limit() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 20"
            );
        }

        var incident = incidents
                .findById(request.incidentId());

        if (incident.isEmpty()) {
            return List.of();
        }

        return List.copyOf(
                deployments.findByServiceAndWindow(
                        incident.get().serviceName(),
                        request.from(),
                        request.to(),
                        request.limit()
                )
        );
    }
}
