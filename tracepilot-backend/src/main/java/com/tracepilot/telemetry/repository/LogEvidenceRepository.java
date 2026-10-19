
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.LogEvidence;

import java.time.Instant;
import java.util.List;

public interface LogEvidenceRepository {

    List<LogEvidence> search(
            String incidentId,
            Instant from,
            Instant to,
            int limit
    );
}
