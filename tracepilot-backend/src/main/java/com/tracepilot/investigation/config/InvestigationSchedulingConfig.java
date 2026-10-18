
package com.tracepilot.investigation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("postgres")
@EnableScheduling
public class InvestigationSchedulingConfig {
}
