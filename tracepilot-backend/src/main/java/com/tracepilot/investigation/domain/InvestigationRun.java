
package com.tracepilot.investigation.domain;

import java.time.Instant;
import java.util.UUID;

public record InvestigationRun(
        UUID id,
        String incidentId,
        InvestigationStatus status,
        Instant startedAt,
        Instant completedAt,
        String failureReason
) {}
