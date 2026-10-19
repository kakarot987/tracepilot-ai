
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Profile("postgres")
public class WorkflowPersistenceService {

    private static final int MAX_STEPS = 8;

    private final JdbcClient jdbc;

    public WorkflowPersistenceService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public WorkflowRunRecord start(String incidentId) {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident ID is required");
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        jdbc.sql("""
                        INSERT INTO workflow_runs
                            (id, incident_id, status, started_at)
                        VALUES (:id, :incidentId, 'PENDING', :now)
                        """)
                .param("id", id)
                .param("incidentId", incidentId)
                .param("now", toDatabaseTime(now))
                .update();

        return get(id);
    }

    @Transactional
    public WorkflowStepRecord beginStep(
            UUID workflowId, AgentRole role) {

        if (role == null) {
            throw new IllegalArgumentException(
                    "Agent role is required");
        }

        // Serialize step allocation for this workflow.
        WorkflowRunRecord run = jdbc.sql("""
                        SELECT *
                        FROM workflow_runs
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", workflowId)
                .query((rs, row) -> mapRun(rs))
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown workflow"));

        if (run.status().isTerminal()) {
            throw new IllegalStateException(
                    "Workflow is terminal");
        }

        if (run.stepsUsed() >= MAX_STEPS) {
            throw new IllegalStateException(
                    "Workflow step limit exceeded");
        }

        // Step 2 permits at most one active agent step.
        if (run.currentRole() != null) {
            throw new IllegalStateException(
                    "Another agent step is already active");
        }

        int number = run.stepsUsed() + 1;
        UUID stepId = UUID.randomUUID();
        Instant now = Instant.now();

        int changed = jdbc.sql("""
                                    UPDATE workflow_runs
                                    SET status = 'RUNNING',
                                        active_agent_role = :role,
                                        steps_used = :number,
                                        version = version + 1
                                    WHERE id = :id
                        """)
                .param("role", role.name())
                .param("number", number)
                .param("id", workflowId)
                .update();

        if (changed != 1) {
            throw new IllegalStateException(
                    "Workflow transition failed");
        }

        jdbc.sql("""
                        INSERT INTO workflow_steps
                            (id, workflow_id, step_number,
                             agent_role, status, started_at)
                        VALUES
                            (:id, :workflowId, :number,
                             :role, 'RUNNING', :now)
                        """)
                .param("id", stepId)
                .param("workflowId", workflowId)
                .param("number", number)
                .param("role", role.name())
                .param("now", toDatabaseTime(now))
                .update();

        return getStep(stepId);
    }

    @Transactional
    public void completeStep(UUID stepId, String summary) {
        finishStep(stepId, WorkflowStepStatus.COMPLETED,
                summary, null);
    }

    @Transactional
    public void failStep(UUID stepId, String errorCode) {
        finishStep(stepId, WorkflowStepStatus.FAILED,
                null, errorCode);
    }

    private void finishStep(
            UUID stepId,
            WorkflowStepStatus desired,
            String summary,
            String errorCode) {

        // Lock the workflow first to maintain a consistent
        // lock order across all lifecycle operations.
        UUID workflowId = jdbc.sql("""
                        SELECT workflow_id
                        FROM workflow_steps
                        WHERE id = :id
                        """)
                .param("id", stepId)
                .query(UUID.class)
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown step"));

        WorkflowRunRecord run = jdbc.sql("""
                        SELECT *
                        FROM workflow_runs
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", workflowId)
                .query((rs, row) -> mapRun(rs))
                .single();

        if (run.status().isTerminal()) {
            throw new IllegalStateException(
                    "Workflow is already terminal");
        }

        WorkflowStepRecord step = jdbc.sql("""
                        SELECT *
                        FROM workflow_steps
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", stepId)
                .query((rs, row) -> mapStep(rs))
                .single();

        if (step.status() != WorkflowStepStatus.RUNNING) {
            throw new IllegalStateException(
                    "Step is not running");
        }

        if (run.currentRole() != step.agentRole()
                || run.stepsUsed() != step.stepNumber()) {
            throw new IllegalStateException(
                    "Step is not the active workflow step");
        }

        Instant now = Instant.now();
        long durationMs = Math.max(0,
                Duration.between(
                        step.startedAt(), now).toMillis());

        jdbc.sql("""
                        UPDATE workflow_steps
                        SET status = :status,
                            completed_at = :now,
                            duration_ms = :duration,
                            output_summary = :summary,
                            error_code = :errorCode
                        WHERE id = :id AND status = 'RUNNING'
                        """)
                .param("status", desired.name())
                .param("now", toDatabaseTime(now))
                .param("duration", durationMs)
                .param("summary", truncate(summary, 2000))
                .param("errorCode", truncate(errorCode, 100))
                .param("id", stepId)
                .update();

        jdbc.sql("""
        UPDATE workflow_runs
        SET active_agent_role = NULL,
            version = version + 1
        WHERE id = :id
        """)
                .param("id", workflowId)
                .update();
    }

    @Transactional
    public void finishWorkflow(
            UUID workflowId, WorkflowStatus status) {

        if (status == null || !status.isTerminal()) {
            throw new IllegalArgumentException(
                    "Terminal workflow status required");
        }

        WorkflowRunRecord run = jdbc.sql("""
                        SELECT *
                        FROM workflow_runs
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", workflowId)
                .query((rs, row) -> mapRun(rs))
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown workflow"));

        if (run.status().isTerminal()
                || run.currentRole() != null) {
            throw new IllegalStateException(
                    "Workflow cannot be finalized");
        }

        jdbc.sql("""
                                    UPDATE workflow_runs
                                    SET status = :status,
                                        completed_at = :now,
                                        active_agent_role = NULL,
                                        version = version + 1
                                    WHERE id = :id
                        """)
                .param("status", status.name())
                .param("now", toDatabaseTime(Instant.now()))
                .param("id", workflowId)
                .update();
    }

    @Transactional(readOnly = true)
    public WorkflowRunRecord get(UUID id) {
        return jdbc.sql("""
                        SELECT *
                        FROM workflow_runs
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, row) -> mapRun(rs))
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown workflow"));
    }

    @Transactional(readOnly = true)
    public WorkflowStepRecord getStep(UUID id) {
        return jdbc.sql("""
                        SELECT *
                        FROM workflow_steps
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, row) -> mapStep(rs))
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown step"));
    }

    @Transactional(readOnly = true)
    public List<WorkflowStepRecord> trace(UUID workflowId) {
        return jdbc.sql("""
                        SELECT *
                        FROM workflow_steps
                        WHERE workflow_id = :id
                        ORDER BY step_number ASC
                        """)
                .param("id", workflowId)
                .query((rs, row) -> mapStep(rs))
                .list();
    }

    private WorkflowRunRecord mapRun(
            java.sql.ResultSet rs)
            throws java.sql.SQLException {

        String role = rs.getString("active_agent_role");
        return new WorkflowRunRecord(
                rs.getObject("id", UUID.class),
                rs.getString("incident_id"),
                WorkflowStatus.valueOf(
                        rs.getString("status")),
                role == null ? null
                        : AgentRole.valueOf(role),
                rs.getInt("steps_used"),
                rs.getInt("follow_ups_used"),
                rs.getLong("version"),
                rs.getTimestamp("started_at").toInstant(),
                timestamp(rs, "completed_at"),
                rs.getString("failure_reason")
        );
    }

    private WorkflowStepRecord mapStep(
            java.sql.ResultSet rs)
            throws java.sql.SQLException {

        long duration = rs.getLong("duration_ms");
        Long durationMs = rs.wasNull()
                ? null : duration;

        return new WorkflowStepRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("workflow_id", UUID.class),
                rs.getInt("step_number"),
                AgentRole.valueOf(
                        rs.getString("agent_role")),
                WorkflowStepStatus.valueOf(
                        rs.getString("status")),
                rs.getTimestamp("started_at").toInstant(),
                timestamp(rs, "completed_at"),
                durationMs,
                rs.getString("output_summary"),
                rs.getString("error_code")
        );
    }

    private Instant timestamp(
            java.sql.ResultSet rs, String name)
            throws java.sql.SQLException {

        var value = rs.getTimestamp(name);
        return value == null ? null : value.toInstant();
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return null;
        }
        return value.length() <= limit
                ? value : value.substring(0, limit);
    }

    private OffsetDateTime toDatabaseTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
