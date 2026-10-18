
package com.tracepilot.investigation.config;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallingConfiguration {

    @Bean
    @ConditionalOnMissingBean(ToolCallingManager.class)
    ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder().build();
    }
}
