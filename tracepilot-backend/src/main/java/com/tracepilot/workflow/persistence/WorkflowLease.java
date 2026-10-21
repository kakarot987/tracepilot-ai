
package com.tracepilot.workflow.persistence;

import java.time.Instant;
import java.util.UUID;

public record WorkflowLease(
        UUID workflowId,
        UUID ownerId,
        long fencingToken,
        Instant expiresAt
) {
    public WorkflowLease {
        if (workflowId == null || ownerId == null
                || expiresAt == null || fencingToken < 1) {
            throw new IllegalArgumentException(
                    "Invalid workflow lease");
        }
    }
}
