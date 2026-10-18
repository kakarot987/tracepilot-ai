
package com.tracepilot.investigation.domain;

import java.time.Instant;
import java.util.UUID;

public record EvidenceReference(
        UUID investigationId,
        String evidenceId,
        EvidenceType evidenceType,
        Instant recordedAt
) {}
