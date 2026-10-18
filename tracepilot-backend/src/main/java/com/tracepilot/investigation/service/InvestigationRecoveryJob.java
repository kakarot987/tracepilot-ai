
package com.tracepilot.investigation.service;

import com.tracepilot.investigation.repository.InvestigationRunRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@Profile("postgres")
public class InvestigationRecoveryJob {

    // 90-second investigation deadline plus 30 seconds
    // of recovery grace.
    private static final Duration STALE_AFTER =
            Duration.ofSeconds(120);

    private final InvestigationRunRepository runs;
    private final Clock clock = Clock.systemUTC();

    public InvestigationRecoveryJob(
            InvestigationRunRepository runs) {
        this.runs = runs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recover();
    }

    @Scheduled(fixedDelay = 30000)
    public void recoverPeriodically() {
        recover();
    }

    public int recover() {
        Instant cutoff = clock.instant().minus(STALE_AFTER);
        return runs.expireStaleRuns(cutoff);
    }
}
