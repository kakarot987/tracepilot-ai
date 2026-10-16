
package com.tracepilot.tools.logs;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.telemetry.domain.LogEvidence;
import com.tracepilot.telemetry.repository.LogEvidenceRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@Profile("simulator")
public class SearchLogsTool {

    private static final int MAX_ROWS = 50;
    private static final Duration MAX_WINDOW = Duration.ofHours(24);

    private final IncidentRepository incidents;
    private final LogEvidenceRepository logs;

    public SearchLogsTool(
            IncidentRepository incidents,
            LogEvidenceRepository logs) {
        this.incidents = incidents;
        this.logs = logs;
    }

    public List<LogEvidence> search(SearchLogsRequest request) {
        Objects.requireNonNull(request, "request is required");

        String incidentId = request.incidentId();
        Instant from = request.from();
        Instant to = request.to();
        Integer limit = request.limit();

        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException(
                    "incidentId is required"
            );
        }

        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "from must be before to"
            );
        }

        if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException(
                    "Time window cannot exceed 24 hours"
            );
        }

        if (limit == null || limit < 1 || limit > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 50"
            );
        }

        // Never retrieve logs for an unknown incident.
        if (incidents.findById(incidentId).isEmpty()) {
            return List.of();
        }

        return List.copyOf(
                logs.search(incidentId, from, to, limit)
        );
    }
}
