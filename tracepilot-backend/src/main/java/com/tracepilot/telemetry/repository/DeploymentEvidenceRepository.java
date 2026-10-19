
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.DeploymentEvidence;

import java.time.Instant;
import java.util.List;

public interface DeploymentEvidenceRepository {

    List<DeploymentEvidence> findByServiceAndWindow(
            String serviceName,
            Instant from,
            Instant to,
            int limit
    );
}
