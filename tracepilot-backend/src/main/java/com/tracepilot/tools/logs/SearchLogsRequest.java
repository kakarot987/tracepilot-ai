
package com.tracepilot.tools.logs;

import java.time.Instant;

public record SearchLogsRequest(
        String incidentId,
        Instant from,
        Instant to,
        Integer limit
) {}
