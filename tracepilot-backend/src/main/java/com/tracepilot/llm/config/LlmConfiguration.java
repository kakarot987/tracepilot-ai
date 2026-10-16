
package com.tracepilot.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmReliabilityProperties.class)
public class LlmConfiguration {
}
