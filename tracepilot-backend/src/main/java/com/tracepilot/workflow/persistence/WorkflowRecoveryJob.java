
package com.tracepilot.workflow.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class WorkflowRecoveryJob {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WorkflowRecoveryJob.class);

    private final WorkflowPersistenceService workflows;
    private final AtomicLong recoveredTotal = new AtomicLong();
    private final AtomicLong recoveryFailures = new AtomicLong();

    public WorkflowRecoveryJob(
            WorkflowPersistenceService workflows) {

        this.workflows = workflows;
    }

    public long recoveredTotal() {
        return recoveredTotal.get();
    }

    public long recoveryFailures() {
        return recoveryFailures.get();
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void recover() {

        int count;
        try {
            count = workflows.recoverExpiredWorkflows();
        } catch (RuntimeException exception) {
            recoveryFailures.incrementAndGet();
            log.error("Workflow recovery sweep failed; failureCount={}",
                    recoveryFailures.get(), exception);
            throw exception;
        }
        if (count < 0) {
            throw new IllegalStateException("Recovery count cannot be negative");
        }
        recoveredTotal.addAndGet(count);

        if (count > 0) {
            log.warn(
                    "Recovered {} expired workflows; recoveredTotal={}",
                    count, recoveredTotal.get()
            );
        }
    }
}
