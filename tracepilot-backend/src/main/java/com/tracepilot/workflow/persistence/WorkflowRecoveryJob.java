
package com.tracepilot.workflow.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class WorkflowRecoveryJob {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WorkflowRecoveryJob.class);

    private final WorkflowPersistenceService workflows;

    public WorkflowRecoveryJob(
            WorkflowPersistenceService workflows) {

        this.workflows = workflows;
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void recover() {

        int count = workflows.recoverExpiredWorkflows();

        if (count > 0) {
            log.warn(
                    "Recovered {} expired workflows",
                    count
            );
        }
    }
}
