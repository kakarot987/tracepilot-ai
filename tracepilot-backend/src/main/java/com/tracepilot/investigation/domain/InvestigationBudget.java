
package com.tracepilot.investigation.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class InvestigationBudget {

    private final int maxToolCalls;
    private final long deadlineNanos;
    private final AtomicInteger calls = new AtomicInteger();

    public InvestigationBudget(
            int maxToolCalls,
            Duration maxDuration) {

        Objects.requireNonNull(maxDuration);

        if (maxToolCalls < 1 || maxToolCalls > 20) {
            throw new IllegalArgumentException(
                    "maxToolCalls must be between 1 and 20"
            );
        }

        if (maxDuration.isZero() || maxDuration.isNegative()
                || maxDuration.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException(
                    "maxDuration must be between 1ns and 5 minutes"
            );
        }

        this.maxToolCalls = maxToolCalls;
        this.deadlineNanos =
                System.nanoTime() + maxDuration.toNanos();
    }

    public void claimToolCall() {
        if (isExpired()) {
            throw new InvestigationLimitExceededException(
                    "Investigation deadline exceeded"
            );
        }

        int current = calls.incrementAndGet();

        if (current > maxToolCalls) {
            throw new InvestigationLimitExceededException(
                    "Maximum tool calls exceeded"
            );
        }
    }

    public boolean isExpired() {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    public int toolCallsUsed() {
        return Math.min(calls.get(), maxToolCalls);
    }

    public int remainingToolCalls() {
        return Math.max(
                0, maxToolCalls - calls.get()
        );
    }
}
