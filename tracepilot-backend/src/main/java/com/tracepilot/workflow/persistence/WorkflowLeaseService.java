
package com.tracepilot.workflow.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("postgres")
public class WorkflowLeaseService {

    private static final int LEASE_SECONDS = 60;

    private final JdbcClient jdbc;

    public WorkflowLeaseService(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional
    public Optional<WorkflowLease> acquire(
            UUID workflowId,
            UUID ownerId) {

        Objects.requireNonNull(workflowId);
        Objects.requireNonNull(ownerId);

        return jdbc.sql("""
                UPDATE workflow_runs
                SET execution_owner = :owner,
                    lease_expires_at =
                        clock_timestamp() + INTERVAL '60 seconds',
                    fencing_token = fencing_token + 1,
                    version = version + 1
                WHERE id = :workflow
                  AND status IN ('PENDING', 'RUNNING')
                  AND started_at >
                      clock_timestamp() - INTERVAL '4 minutes'
                  AND (
                      execution_owner IS NULL
                      OR lease_expires_at <= clock_timestamp()
                  )
                RETURNING id, execution_owner,
                          fencing_token, lease_expires_at
                """)
                .param("workflow", workflowId)
                .param("owner", ownerId)
                .query((rs, row) -> new WorkflowLease(
                        rs.getObject("id", UUID.class),
                        rs.getObject("execution_owner", UUID.class),
                        rs.getLong("fencing_token"),
                        rs.getObject(
                                "lease_expires_at",
                                OffsetDateTime.class
                        ).toInstant()
                ))
                .optional();
    }

    @Transactional
    public boolean renew(WorkflowLease lease) {

        Objects.requireNonNull(lease);

        return jdbc.sql("""
                UPDATE workflow_runs
                SET lease_expires_at =
                        clock_timestamp() + INTERVAL '60 seconds',
                    version = version + 1
                WHERE id = :workflow
                  AND execution_owner = :owner
                  AND fencing_token = :token
                  AND lease_expires_at > clock_timestamp()
                  AND status IN ('PENDING', 'RUNNING')
                  AND started_at >
                      clock_timestamp() - INTERVAL '4 minutes'
                """)
                .param("workflow", lease.workflowId())
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .update() == 1;
    }

    @Transactional
    public boolean release(WorkflowLease lease) {

        Objects.requireNonNull(lease);

        return jdbc.sql("""
                UPDATE workflow_runs
                SET execution_owner = NULL,
                    lease_expires_at = NULL,
                    version = version + 1
                WHERE id = :workflow
                  AND execution_owner = :owner
                  AND fencing_token = :token
                """)
                .param("workflow", lease.workflowId())
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .update() == 1;
    }

    @Transactional(readOnly = true)
    public boolean isOwner(WorkflowLease lease) {

        Objects.requireNonNull(lease);

        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM workflow_runs
                    WHERE id = :workflow
                      AND execution_owner = :owner
                      AND fencing_token = :token
                      AND lease_expires_at > clock_timestamp()
                      AND status IN ('PENDING', 'RUNNING')
                )
                """)
                .param("workflow", lease.workflowId())
                .param("owner", lease.ownerId())
                .param("token", lease.fencingToken())
                .query(Boolean.class)
                .single();
    }
}
