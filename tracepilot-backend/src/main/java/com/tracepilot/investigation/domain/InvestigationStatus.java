
package com.tracepilot.investigation.domain;

public enum InvestigationStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    INSUFFICIENT_EVIDENCE;

    public boolean isTerminal() {
        return this != RUNNING;
    }
}
