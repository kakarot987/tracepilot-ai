
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.HistoricalIncident;

import java.time.Instant;
import java.util.List;

public interface IncidentHistoryRepository {

    List<HistoricalIncident> findHistory(
            String serviceName,
            Instant before,
            int limit
    );
}
