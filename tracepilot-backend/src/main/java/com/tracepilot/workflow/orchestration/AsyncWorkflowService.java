package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.persistence.WorkflowLease;
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
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Semaphore capacity = new Semaphore(2);

    public AsyncWorkflowService(WorkflowExecutionStarter starter, WorkflowOrchestrator orchestrator) {
        this.starter = starter;
        this.orchestrator = orchestrator;
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
            // The lease has already been persisted. Do not silently retry this POST.
            log.error("Async workflow dispatch failed; workflowId={}", lease.workflowId(), exception);
            throw exception;
        }
        return lease.workflowId();
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
    }
}
