
package com.tracepilot.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "tracepilot.llm")
public record LlmReliabilityProperties(
        @DefaultValue("1") int maxTransientRetries,
        @DefaultValue("500ms") Duration retryBackoff,
        @DefaultValue("100s") Duration requestDeadline
) {
    public LlmReliabilityProperties {
        if (maxTransientRetries < 0 || maxTransientRetries > 1) {
            throw new IllegalArgumentException(
                    "maxTransientRetries must be 0 or 1"
            );
        }

        if (retryBackoff.isNegative()
                || retryBackoff.isZero()
                || requestDeadline.isNegative()
                || requestDeadline.isZero()) {
            throw new IllegalArgumentException(
                    "Retry and deadline durations must be positive"
            );
        }
    }
}
