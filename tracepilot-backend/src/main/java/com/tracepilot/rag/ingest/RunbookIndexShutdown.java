
package com.tracepilot.rag.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
@ConditionalOnProperty(
        name = "tracepilot.rag.index-enabled",
        havingValue = "true"
)
public class RunbookIndexShutdown {

    private final ConfigurableApplicationContext context;

    public RunbookIndexShutdown(
            ConfigurableApplicationContext context) {
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void shutdownAfterIndexing() {
        // ApplicationRunner executes before ApplicationReadyEvent.
        // This listener runs only after successful startup
        // and completion of the indexing runner.
        SpringApplication.exit(context);
    }
}
