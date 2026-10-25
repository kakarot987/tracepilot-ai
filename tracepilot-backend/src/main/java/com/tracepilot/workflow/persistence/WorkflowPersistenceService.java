
package com.tracepilot.workflow.persistence;

import com.tracepilot.workflow.domain.AgentRole;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.domain.WorkflowStepStatus;
import com.tracepilot.workflow.domain.AgentRole;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Profile("postgres")
public class WorkflowPersistenceService {

    private static final int MAX_STEPS =
            WorkflowLimits.MAX_STEPS;

    private final JdbcClient jdbc;

    private final JsonMapper jsonMapper;

    public WorkflowPersistenceService(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
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
            UUID workflowId,
            AgentRole role,
            WorkflowLease lease) {

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

        requireBeforeDeadline(run);
        requireOwnership(workflowId, lease);

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
    public void completeStep(UUID stepId, String summary,
                             WorkflowLease lease) {
        finishStep(stepId, WorkflowStepStatus.COMPLETED,
                summary, null,lease);
    }

    @Transactional
    public void failStep(UUID stepId, String errorCode,
                         WorkflowLease lease) {
        finishStep(stepId, WorkflowStepStatus.FAILED,
                null, errorCode, lease);
    }

    private void finishStep(
            UUID stepId,
            WorkflowStepStatus desired,
            String summary,
            String errorCode,
            WorkflowLease lease) {

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

        if (desired == WorkflowStepStatus.COMPLETED) {
            requireBeforeDeadline(run);
        }
        requireOwnership(workflowId, lease);

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
            UUID workflowId, WorkflowStatus status, WorkflowLease lease) {

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

        if (status == WorkflowStatus.COMPLETED) {
            requireBeforeDeadline(run);
        }
        requireOwnership(workflowId, lease);

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

    @Transactional
    public boolean reserveFollowUp(
            UUID workflowId,
            WorkflowLease lease) {

        Objects.requireNonNull(lease);

        if (!workflowId.equals(lease.workflowId())) {
            throw new IllegalArgumentException(
                    "Lease belongs to another workflow");
        }

        int changed = jdbc.sql("""
            UPDATE workflow_runs
            SET follow_ups_used = follow_ups_used + 1,
                version = version + 1
            WHERE id = :id
              AND status = 'RUNNING'
              AND active_agent_role IS NULL
              AND follow_ups_used < 1
              AND started_at >
                  clock_timestamp() - INTERVAL '4 minutes'
              AND execution_owner = :owner
              AND fencing_token = :token
              AND lease_expires_at > clock_timestamp()
            """)
                .param("id", workflowId)
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .update();

        return changed == 1;
    }

    @Transactional
    public void completeStepWithOutput(
            UUID stepId,
            AgentRole expectedRole,
            String summary,
            Object output,
            UUID investigationRunId,
            WorkflowLease lease) {

        if (stepId == null || expectedRole == null
                || output == null) {
            throw new IllegalArgumentException(
                    "Step, role and output are required");
        }

        String json = jsonMapper.writeValueAsString(output);

        if (json.isBlank() || json.length() > 16000) {
            throw new IllegalArgumentException(
                    "Step output exceeds bounds");
        }

        // Lock ordering matches finishStep():
        // workflow first, then step.
        UUID workflowId = jdbc.sql("""
            SELECT workflow_id
            FROM workflow_steps
            WHERE id = :stepId
            """)
                .param("stepId", stepId)
                .query(UUID.class)
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown step"));

        var run = jdbc.sql("""
            SELECT *
            FROM workflow_runs
            WHERE id = :workflowId
            FOR UPDATE
            """)
                .param("workflowId", workflowId)
                .query((rs, row) -> mapRun(rs))
                .single();

        var step = jdbc.sql("""
            SELECT *
            FROM workflow_steps
            WHERE id = :stepId
            FOR UPDATE
            """)
                .param("stepId", stepId)
                .query((rs, row) -> mapStep(rs))
                .single();

        if (run.status().isTerminal()
                || run.currentRole() != expectedRole
                || step.agentRole() != expectedRole
                || step.status() != WorkflowStepStatus.RUNNING
                || run.stepsUsed() != step.stepNumber()) {
            throw new IllegalStateException(
                    "Step cannot be completed with output");
        }

        requireBeforeDeadline(run);
        requireOwnership(workflowId, lease);

        if (investigationRunId != null) {
            if (expectedRole != AgentRole.INVESTIGATOR) {
                throw new IllegalArgumentException(
                        "Only Investigator steps link child runs");
            }

            boolean belongsToIncident = jdbc.sql("""
                SELECT COUNT(*)
                FROM investigation_runs
                WHERE id = :investigationId
                  AND incident_id = :incidentId
                """)
                    .param("investigationId", investigationRunId)
                    .param("incidentId", run.incidentId())
                    .query(Long.class)
                    .single() == 1L;

            if (!belongsToIncident) {
                throw new IllegalArgumentException(
                        "Child investigation belongs to another incident");
            }
        }

        if (expectedRole == AgentRole.INVESTIGATOR
                && investigationRunId == null) {
            throw new IllegalArgumentException(
                    "Investigator child run ID is required");
        }

        Instant now = Instant.now();

        long durationMs = Math.max(
                0,
                Duration.between(
                        step.startedAt(), now).toMillis()
        );

        int changed = jdbc.sql("""
            UPDATE workflow_steps
            SET status = 'COMPLETED',
                completed_at = :now,
                duration_ms = :duration,
                output_summary = :summary,
                output_json = CAST(:json AS jsonb),
                output_schema_version = 1,
                investigation_run_id = :investigationId
            WHERE id = :stepId
              AND status = 'RUNNING'
            """)
                .param("now", toDatabaseTime(now))
                .param("duration", durationMs)
                .param("summary", truncate(summary, 2000))
                .param("json", json)
                .param("investigationId",
                        investigationRunId,
                        java.sql.Types.OTHER)
                .param("stepId", stepId)
                .update();

        if (changed != 1) {
            throw new IllegalStateException(
                    "Step completion update failed");
        }

        int updated = jdbc.sql("""
            UPDATE workflow_runs
            SET active_agent_role = NULL,
                version = version + 1
            WHERE id = :workflowId
            """)
                .param("workflowId", workflowId)
                .update();

        if (updated != 1) {
            throw new IllegalStateException(
                    "Workflow transition failed");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<WorkflowStepOutput> output(
            UUID stepId) {

        return jdbc.sql("""
            SELECT
                id,
                workflow_id,
                agent_role,
                output_schema_version,
                output_json::text AS output_text,
                investigation_run_id
            FROM workflow_steps
            WHERE id = :stepId
              AND status = 'COMPLETED'
              AND output_json IS NOT NULL
            """)
                .param("stepId", stepId)
                .query((rs, row) -> new WorkflowStepOutput(
                        rs.getObject("id", UUID.class),
                        rs.getObject("workflow_id", UUID.class),
                        AgentRole.valueOf(
                                rs.getString("agent_role")),
                        rs.getInt("output_schema_version"),
                        rs.getString("output_text"),
                        rs.getObject(
                                "investigation_run_id",
                                UUID.class)
                ))
                .optional();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<WorkflowStepOutput> latestOutput(
            UUID workflowId,
            AgentRole role) {

        return jdbc.sql("""
            SELECT
                id,
                workflow_id,
                agent_role,
                output_schema_version,
                output_json::text AS output_text,
                investigation_run_id
            FROM workflow_steps
            WHERE workflow_id = :workflowId
              AND agent_role = :role
              AND status = 'COMPLETED'
              AND output_json IS NOT NULL
            ORDER BY step_number DESC
            LIMIT 1
            """)
                .param("workflowId", workflowId)
                .param("role", role.name())
                .query((rs, row) -> new WorkflowStepOutput(
                        rs.getObject("id", UUID.class),
                        rs.getObject("workflow_id", UUID.class),
                        AgentRole.valueOf(
                                rs.getString("agent_role")),
                        rs.getInt("output_schema_version"),
                        rs.getString("output_text"),
                        rs.getObject(
                                "investigation_run_id",
                                UUID.class)
                ))
                .optional();
    }

    /**
     * Finalize a leased PENDING workflow when dispatch fails before execution.
     * Fencing guards against changing a run claimed by another worker.
     */
    @Transactional
    public boolean failPendingDispatch(WorkflowLease lease) {
        Objects.requireNonNull(lease);
        return jdbc.sql("""
                UPDATE workflow_runs
                SET status = 'FAILED',
                    completed_at = clock_timestamp(),
                    failure_reason = 'Workflow dispatch failed',
                    execution_owner = NULL,
                    lease_expires_at = NULL,
                    version = version + 1
                WHERE id = :id
                  AND status = 'PENDING'
                  AND active_agent_role IS NULL
                  AND execution_owner = :owner
                  AND fencing_token = :token
                """)
                .param("id", lease.workflowId())
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .update() == 1;
    }

    @Transactional
    public int recoverExpiredWorkflows() {

        Instant cutoff = Instant.now()
                .minus(WorkflowLimits.MAX_DURATION);

        // Lock expired workflows, but do not wait on
        // a workflow currently held by another transaction.
        List<UUID> expiredIds = jdbc.sql("""
            SELECT id
            FROM workflow_runs
            WHERE status IN ('PENDING', 'RUNNING')
              AND started_at <= :cutoff
            ORDER BY started_at, id
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """)
                .param("cutoff", toDatabaseTime(cutoff))
                .query(UUID.class)
                .list();

        for (UUID workflowId : expiredIds) {

            // Close any step abandoned while RUNNING.
            jdbc.sql("""
                UPDATE workflow_steps
                SET status = 'TIMED_OUT',
                    completed_at = GREATEST(
                        clock_timestamp(), started_at),
                    duration_ms = GREATEST(
                        0,
                        (
                            EXTRACT(EPOCH FROM
                                (clock_timestamp() - started_at))
                            * 1000
                        )::bigint
                    ),
                    error_code = 'WORKFLOW_DEADLINE_EXCEEDED'
                WHERE workflow_id = :workflowId
                  AND status = 'RUNNING'
                """)
                    .param("workflowId", workflowId)
                    .update();

            int changed = jdbc.sql("""
                UPDATE workflow_runs
                SET status = 'TIMED_OUT',
                    active_agent_role = NULL,
                    completed_at = GREATEST(
                        clock_timestamp(), started_at),
                    failure_reason = 'Workflow deadline exceeded',
                    version = version + 1
                WHERE id = :workflowId
                  AND status IN ('PENDING', 'RUNNING')
                """)
                    .param("workflowId", workflowId)
                    .update();

            if (changed != 1) {
                throw new IllegalStateException(
                        "Expired workflow transition failed");
            }
        }

        return expiredIds.size();
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


    private void requireBeforeDeadline(WorkflowRunRecord run) {

        Instant deadline = run.startedAt()
                .plus(WorkflowLimits.MAX_DURATION);

        if (!Instant.now().isBefore(deadline)) {
            throw new IllegalStateException(
                    "Workflow deadline exceeded");
        }
    }

    private void requireOwnership(
            UUID workflowId,
            WorkflowLease lease) {

        if (lease == null
                || !workflowId.equals(lease.workflowId())) {
            throw new IllegalStateException(
                    "Workflow lease does not match workflow");
        }

        boolean valid = jdbc.sql("""
            SELECT EXISTS (
                SELECT 1
                FROM workflow_runs
                WHERE id = :workflowId
                  AND execution_owner = :owner
                  AND fencing_token = :token
                  AND lease_expires_at > clock_timestamp()
                  AND status IN ('PENDING', 'RUNNING')
            )
            """)
                .param("workflowId", workflowId)
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .query(Boolean.class)
                .single();

        if (!valid) {
            throw new IllegalStateException(
                    "Workflow execution ownership lost");
        }
    }

}
