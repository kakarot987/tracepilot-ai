package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncWorkflowServiceTest {
    @Test
    void executorRejectionFinalizesPersistedPendingWorkflow() {
        var starter = mock(WorkflowExecutionStarter.class);
        var orchestrator = mock(WorkflowOrchestrator.class);
        var workflows = mock(WorkflowPersistenceService.class);
        var executor = mock(ExecutorService.class);
        var lease = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        when(starter.start("INC-DB-001")).thenReturn(lease);
        doThrow(new RejectedExecutionException("shutdown")).when(executor).execute(any());
        when(workflows.failPendingDispatch(lease)).thenReturn(true);
        var service = new AsyncWorkflowService(starter, orchestrator, workflows, executor, 2);

        assertThrows(RejectedExecutionException.class, () -> service.submit("INC-DB-001"));
        verify(workflows).failPendingDispatch(lease);
        verifyNoInteractions(orchestrator);
        service.close();
    }

    @Test
    void cleanupFailureIsNotReportedAsOrdinaryCapacityRejection() {
        var starter = mock(WorkflowExecutionStarter.class);
        var orchestrator = mock(WorkflowOrchestrator.class);
        var workflows = mock(WorkflowPersistenceService.class);
        var executor = mock(ExecutorService.class);
        var lease = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        when(starter.start("INC-DB-001")).thenReturn(lease);
        doThrow(new RejectedExecutionException("shutdown")).when(executor).execute(any());
        when(workflows.failPendingDispatch(lease)).thenReturn(false);
        var service = new AsyncWorkflowService(starter, orchestrator, workflows, executor, 2);

        assertThrows(IllegalStateException.class, () -> service.submit("INC-DB-001"));
        verify(workflows).failPendingDispatch(lease);
        service.close();
    }
    @Test
    void startFailureReleasesCapacityForNextSubmission() {
        var starter = mock(WorkflowExecutionStarter.class);
        var orchestrator = mock(WorkflowOrchestrator.class);
        var workflows = mock(WorkflowPersistenceService.class);
        var executor = mock(ExecutorService.class);
        var lease = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        when(starter.start("INC-DB-001"))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(lease);
        var service = new AsyncWorkflowService(starter, orchestrator, workflows, executor, 1);

        assertThrows(IllegalStateException.class, () -> service.submit("INC-DB-001"));
        assertEquals(lease.workflowId(), service.submit("INC-DB-001"));
        verify(starter, times(2)).start("INC-DB-001");
        verify(executor).execute(any());
        service.close();
    }

    @Test
    void workerFailureReleasesCapacityForNextSubmission() {
        var starter = mock(WorkflowExecutionStarter.class);
        var orchestrator = mock(WorkflowOrchestrator.class);
        var workflows = mock(WorkflowPersistenceService.class);
        var executor = mock(ExecutorService.class);
        var first = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        var second = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        when(starter.start("INC-DB-001")).thenReturn(first, second);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        doThrow(new IllegalStateException("model failed"))
                .when(orchestrator).execute(first);
        var service = new AsyncWorkflowService(starter, orchestrator, workflows, executor, 1);

        assertEquals(first.workflowId(), service.submit("INC-DB-001"));
        assertEquals(second.workflowId(), service.submit("INC-DB-001"));
        verify(orchestrator).execute(first);
        verify(orchestrator).execute(second);
        service.close();
    }

    @Test
    void capacityRejectionDoesNotCreateAnotherPersistedRun() {
        var starter = mock(WorkflowExecutionStarter.class);
        var orchestrator = mock(WorkflowOrchestrator.class);
        var workflows = mock(WorkflowPersistenceService.class);
        var executor = mock(ExecutorService.class);
        var first = new WorkflowLease(UUID.randomUUID(), UUID.randomUUID(), 1L,
                Instant.now().plusSeconds(60));
        when(starter.start("INC-DB-001")).thenReturn(first);
        var service = new AsyncWorkflowService(starter, orchestrator, workflows, executor, 1);

        assertEquals(first.workflowId(), service.submit("INC-DB-001"));
        assertThrows(RejectedExecutionException.class,
                () -> service.submit("INC-DB-001"));
        verify(starter, times(1)).start("INC-DB-001");
        verify(executor, times(1)).execute(any());
        verifyNoInteractions(workflows, orchestrator);
        service.close();
    }

}
