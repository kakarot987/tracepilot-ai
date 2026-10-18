
package com.tracepilot.investigation.repository;

import com.tracepilot.investigation.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("postgres")
public class JdbcInvestigationRunRepository
        implements InvestigationRunRepository {

    private final JdbcClient jdbc;

    private static final RowMapper<InvestigationRun> RUN_MAPPER =
            (rs, rowNum) -> new InvestigationRun(
                    rs.getObject("id", UUID.class),
                    rs.getString("incident_id"),
                    InvestigationStatus.valueOf(rs.getString("status")),
                    rs.getObject("started_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("completed_at") == null
                            ? null
                            : rs.getObject(
                            "completed_at", OffsetDateTime.class
                    ).toInstant(),
                    rs.getString("failure_reason")
            );

    public JdbcInvestigationRunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insert(InvestigationRun run) {
        int inserted = jdbc.sql("""
                INSERT INTO investigation_runs
                    (id, incident_id, status, started_at)
                VALUES
                    (:id, :incidentId, 'RUNNING', :startedAt)
                ON CONFLICT DO NOTHING
                """)
                .param("id", run.id())
                .param("incidentId", run.incidentId())
                .param(
                        "startedAt",
                        java.time.OffsetDateTime.ofInstant(
                                run.startedAt(),
                                java.time.ZoneOffset.UTC
                        )
                )
                .update();

        return inserted == 1;
    }

    @Override
    public Optional<InvestigationRun> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, incident_id, status,
                       started_at, completed_at, failure_reason
                FROM investigation_runs
                WHERE id = :id
                """)
                .param("id", id)
                .query(RUN_MAPPER)
                .optional();
    }

    @Override
    public boolean finish(
            UUID id,
            InvestigationStatus status,
            String failureReason) {

        if (!status.isTerminal()) {
            throw new IllegalArgumentException(
                    "Target status must be terminal"
            );
        }

        int updated = jdbc.sql("""
                UPDATE investigation_runs
                SET status = :status,
                    completed_at = CURRENT_TIMESTAMP,
                    failure_reason = :failureReason
                WHERE id = :id
                  AND status = 'RUNNING'
                """)
                .param("id", id)
                .param("status", status.name())
                .param("failureReason", failureReason)
                .update();

        return updated == 1;
    }

    @Override
    public void addEvidence(
            UUID investigationId,
            String evidenceId,
            EvidenceType type) {

        jdbc.sql("""
                INSERT INTO investigation_evidence
                    (investigation_id, evidence_id, evidence_type)
                VALUES (:investigationId, :evidenceId, :type)
                ON CONFLICT DO NOTHING
                """)
                .param("investigationId", investigationId)
                .param("evidenceId", evidenceId)
                .param("type", type.name())
                .update();
    }

    @Override
    public List<EvidenceReference> findEvidence(
            UUID investigationId) {

        return jdbc.sql("""
                SELECT investigation_id, evidence_id,
                       evidence_type, recorded_at
                FROM investigation_evidence
                WHERE investigation_id = :investigationId
                ORDER BY recorded_at, evidence_id
                LIMIT 500
                """)
                .param("investigationId", investigationId)
                .query((rs, rowNum) -> new EvidenceReference(
                        rs.getObject("investigation_id", UUID.class),
                        rs.getString("evidence_id"),
                        EvidenceType.valueOf(
                                rs.getString("evidence_type")
                        ),
                        rs.getObject(
                                "recorded_at", OffsetDateTime.class
                        ).toInstant()
                ))
                .list();
    }

    @Override
    public boolean lockIfRunning(UUID id) {
        return jdbc.sql("""
            SELECT status
            FROM investigation_runs
            WHERE id = :id
            FOR UPDATE
            """)
                .param("id", id)
                .query((rs, rowNum) ->
                        "RUNNING".equals(rs.getString("status"))
                )
                .optional()
                .orElse(false);
    }

}
