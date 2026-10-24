
package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.persistence.WorkflowLeaseService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
class WorkflowExecutionStarterIntegrationTest {

    @Autowired
    private WorkflowExecutionStarter starter;

    @Autowired
    private JdbcClient jdbc;

    @MockitoBean
    private WorkflowLeaseService leases;

    @Test
    void rollsBackWorkflowWhenInitialLeaseIsRejected() {

        long before = countWorkflows();

        when(leases.acquire(
                any(UUID.class),
                any(UUID.class)
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> starter.start("INC-DB-001")
        );

        assertEquals(before, countWorkflows());

        verify(leases).acquire(
                any(UUID.class),
                any(UUID.class)
        );
    }

    private long countWorkflows() {
        return jdbc.sql("""
                SELECT COUNT(*)
                FROM workflow_runs
                WHERE incident_id = 'INC-DB-001'
                """)
                .query(Long.class)
                .single();
    }
}
