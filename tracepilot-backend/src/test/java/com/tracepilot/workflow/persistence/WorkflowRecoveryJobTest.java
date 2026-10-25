package com.tracepilot.workflow.persistence;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowRecoveryJobTest {
    @Test
    void countsRecoveredRunsAcrossSweeps() {
        var persistence = mock(WorkflowPersistenceService.class);
        when(persistence.recoverExpiredWorkflows()).thenReturn(2, 0, 3);
        var job = new WorkflowRecoveryJob(persistence);
        job.recover();
        job.recover();
        job.recover();
        assertEquals(5, job.recoveredTotal());
        assertEquals(0, job.recoveryFailures());
        verify(persistence, times(3)).recoverExpiredWorkflows();
    }

    @Test
    void exposesFailedSweepWithoutLosingRecoveryHistory() {
        var persistence = mock(WorkflowPersistenceService.class);
        when(persistence.recoverExpiredWorkflows()).thenReturn(1)
                .thenThrow(new IllegalStateException("database unavailable"));
        var job = new WorkflowRecoveryJob(persistence);
        job.recover();
        assertThrows(IllegalStateException.class, job::recover);
        assertEquals(1, job.recoveredTotal());
        assertEquals(1, job.recoveryFailures());
    }
}
