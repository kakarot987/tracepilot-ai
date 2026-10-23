
package com.tracepilot.workflow.api;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.orchestration.WorkflowExecutionResult;
import com.tracepilot.workflow.orchestration.WorkflowOrchestrator;
import com.tracepilot.workflow.persistence.WorkflowLeaseService;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
@Transactional
@Rollback
class WorkflowApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WorkflowPersistenceService workflows;

    @Autowired
    private WorkflowLeaseService leases;

    // Keep PostgreSQL real but prevent Ollama execution.
    @MockitoBean
    private WorkflowOrchestrator orchestrator;

    @Test
    void returnsPersistedWorkflowTraceAndOutput()
            throws Exception {

        var workflow = workflows.start("INC-DB-001");

        var lease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow();

        var step = workflows.beginStep(
                workflow.id(),
                AgentRole.TRIAGE,
                lease
        );

        workflows.completeStepWithOutput(
                step.id(),
                AgentRole.TRIAGE,
                "Database incident classified",
                Map.of(
                        "severity", "HIGH",
                        "category", "DATABASE",
                        "investigationFocus",
                        "Inspect connection pool"
                ),
                null,
                lease
        );

        workflows.finishWorkflow(
                workflow.id(),
                WorkflowStatus.COMPLETED,
                lease
        );

        String id = workflow.id().toString();

        // GET parent status.
        mvc.perform(get("/api/v1/workflows/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id").value(id))
                .andExpect(jsonPath(
                        "$.status").value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stepsUsed").value(1));

        // GET ordered execution trace.
        mvc.perform(get("/api/v1/workflows/{id}/steps", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[0].agentRole").value("TRIAGE"))
                .andExpect(jsonPath(
                        "$[0].status").value("COMPLETED"))
                .andExpect(jsonPath(
                        "$[0].stepNumber").value(1));

        // GET JSONB output.
        mvc.perform(get(
                        "/api/v1/workflows/{id}/outputs/{role}",
                        id,
                        "TRIAGE"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.agentRole").value("TRIAGE"))
                .andExpect(jsonPath(
                        "$.schemaVersion").value(1))
                .andExpect(jsonPath(
                        "$.json").isString());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void postReturnsOrchestratorResultWithoutCallingOllama()
            throws Exception {

        var workflowId = UUID.randomUUID();

        when(orchestrator.execute("INC-DB-001"))
                .thenReturn(new WorkflowExecutionResult(
                        workflowId,
                        WorkflowStatus.COMPLETED,
                        0,
                        null,
                        null
                ));

        mvc.perform(post(
                        "/api/v1/incidents/{incidentId}/workflows",
                        "INC-DB-001"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.workflowId")
                        .value(workflowId.toString()))
                .andExpect(jsonPath(
                        "$.status").value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.followUpsUsed").value(0));

        verify(orchestrator, times(1))
                .execute("INC-DB-001");
    }

    @Test
    void unknownWorkflowReturns404()
            throws Exception {

        mvc.perform(get(
                        "/api/v1/workflows/{id}",
                        UUID.randomUUID()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownIncidentDoesNotStartWorkflow()
            throws Exception {

        mvc.perform(post(
                        "/api/v1/incidents/{incidentId}/workflows",
                        "INC-DOES-NOT-EXIST"
                ))
                .andExpect(status().isNotFound());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void missingOutputReturns404()
            throws Exception {

        var workflow = workflows.start("INC-DB-001");

        mvc.perform(get(
                        "/api/v1/workflows/{id}/outputs/{role}",
                        workflow.id(),
                        "ROOT_CAUSE"
                ))
                .andExpect(status().isNotFound());
    }
}
