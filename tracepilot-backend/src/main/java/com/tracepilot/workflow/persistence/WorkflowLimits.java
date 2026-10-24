
package com.tracepilot.workflow.persistence;

import java.time.Duration;

public final class WorkflowLimits {

    public static final int MAX_STEPS = 8;
    public static final int MAX_FOLLOW_UPS = 1;
    public static final int MAX_WORKFLOW_TOOL_CALLS = 12;
    public static final int INITIAL_TOOL_CALLS = 8;

    public static final Duration MAX_DURATION =
            Duration.ofMinutes(4);

    private WorkflowLimits() {
    }
}
