
package com.tracepilot.tools.service;

import com.tracepilot.incident.domain.ServiceInfo;
import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.incident.repository.ServiceInfoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@Profile("simulator")
public class GetServiceInfoTool {

    private final IncidentRepository incidents;
    private final ServiceInfoRepository services;

    public GetServiceInfoTool(
            IncidentRepository incidents,
            ServiceInfoRepository services) {
        this.incidents = incidents;
        this.services = services;
    }

    public Optional<ServiceInfo> get(String incidentId) {
        Objects.requireNonNull(incidentId, "incidentId is required");

        if (incidentId.isBlank() || incidentId.length() > 100) {
            throw new IllegalArgumentException(
                    "incidentId must contain 1 to 100 characters"
            );
        }

        return incidents.findById(incidentId)
                .flatMap(incident ->
                        services.findByServiceName(
                                incident.serviceName()
                        )
                );
    }
}
