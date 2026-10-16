
package com.tracepilot.tools.history;

import com.tracepilot.incident.domain.HistoricalIncident;
import com.tracepilot.incident.repository.IncidentHistoryRepository;
import com.tracepilot.incident.repository.IncidentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Profile("simulator")
public class GetIncidentHistoryTool {

    private static final int MAX_ROWS = 10;

    private final IncidentRepository incidents;
    private final IncidentHistoryRepository history;

    public GetIncidentHistoryTool(
            IncidentRepository incidents,
            IncidentHistoryRepository history) {
        this.incidents = incidents;
        this.history = history;
    }

    public List<HistoricalIncident> get(
            GetIncidentHistoryRequest request) {

        Objects.requireNonNull(request, "request is required");

        if (request.incidentId() == null
                || request.incidentId().isBlank()
                || request.incidentId().length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid incidentId"
            );
        }

        if (request.limit() == null
                || request.limit() < 1
                || request.limit() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 10"
            );
        }

        var incident = incidents.findById(request.incidentId());

        if (incident.isEmpty()) {
            return List.of();
        }

        return List.copyOf(
                history.findHistory(
                        incident.get().serviceName(),
                        incident.get().occurredAt(),
                        request.limit()
                )
        );
    }
}
