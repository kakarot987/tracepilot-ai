
package com.tracepilot.workflow.domain;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    INSUFFICIENT_EVIDENCE,
    FAILED,
    TIMED_OUT,
    CANCELLED;

    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, INSUFFICIENT_EVIDENCE,
                 FAILED, TIMED_OUT, CANCELLED -> true;
            default -> false;
        };
    }
}
