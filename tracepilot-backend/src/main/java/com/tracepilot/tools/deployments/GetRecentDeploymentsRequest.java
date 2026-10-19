
package com.tracepilot.tools.deployments;

import java.time.Instant;

public record GetRecentDeploymentsRequest(
        String incidentId,
        Instant from,
        Instant to,
        Integer limit
) {}
