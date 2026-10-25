package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.RejectedExecutionException;

@Service
@Profile("postgres")
public class AsyncWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(AsyncWorkflowService.class);
    private final WorkflowExecutionStarter starter;
    private final WorkflowOrchestrator orchestrator;
    private final WorkflowPersistenceService workflows;
    private final ExecutorService executor;
    private final Semaphore capacity;

    public AsyncWorkflowService(WorkflowExecutionStarter starter,
                                WorkflowOrchestrator orchestrator,
                                WorkflowPersistenceService workflows) {
        this(starter, orchestrator, workflows, Executors.newFixedThreadPool(2), 2);
    }

    AsyncWorkflowService(WorkflowExecutionStarter starter,
                         WorkflowOrchestrator orchestrator,
                         WorkflowPersistenceService workflows,
                         ExecutorService executor, int maxConcurrent) {
        this.starter = starter;
        this.orchestrator = orchestrator;
        this.workflows = workflows;
        this.executor = executor;
        this.capacity = new Semaphore(maxConcurrent);
    }

    public UUID submit(String incidentId) {
        if (!capacity.tryAcquire()) {
            throw new RejectedExecutionException("Maximum concurrent workflows reached");
        }

        WorkflowLease lease;
        try {
            lease = starter.start(incidentId);
        } catch (RuntimeException exception) {
            capacity.release();
            throw exception;
        }

        try {
            executor.execute(() -> {
                try {
                    orchestrator.execute(lease);
                } catch (RuntimeException exception) {
                    log.error("Async workflow failed; workflowId={}", lease.workflowId(), exception);
                } finally {
                    capacity.release();
                }
            });
        } catch (RuntimeException exception) {
            capacity.release();
            try {
                if (!workflows.failPendingDispatch(lease)) {
                    throw new IllegalStateException("Could not finalize rejected workflow");
                }
            } catch (RuntimeException cleanup) {
                exception.addSuppressed(cleanup);
                log.error("Failed to finalize rejected workflow; workflowId={}",
                        lease.workflowId(), cleanup);
                // Never report this as an ordinary rejected submission.
                throw new IllegalStateException(
                        "Workflow dispatch and cleanup failed; workflowId=" + lease.workflowId(),
                        exception);
            }
            log.error("Async workflow dispatch failed; workflowId={}", lease.workflowId(), exception);
            throw new RejectedExecutionException("Workflow dispatch failed", exception);
        }
        return lease.workflowId();
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
    }
}
