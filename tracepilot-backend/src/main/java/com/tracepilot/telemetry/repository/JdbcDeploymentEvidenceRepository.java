
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.DeploymentEvidence;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@Profile("postgres")
public class JdbcDeploymentEvidenceRepository
        implements DeploymentEvidenceRepository {

    private final JdbcTemplate jdbc;

    public JdbcDeploymentEvidenceRepository(
            DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(3);
    }

    @Override
    public List<DeploymentEvidence> findByServiceAndWindow(
            String serviceName,
            Instant from,
            Instant to,
            int limit) {

        if (serviceName == null
                || serviceName.isBlank()
                || serviceName.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid serviceName"
            );
        }

        if (from == null || to == null
                || !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "Invalid time window"
            );
        }

        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 50"
            );
        }

        return jdbc.query("""
                SELECT evidence_id, service_name,
                       version, deployed_at,
                       status, change_summary
                FROM deployment_evidence
                WHERE service_name = ?
                  AND deployed_at >= ?
                  AND deployed_at <= ?
                ORDER BY deployed_at DESC, evidence_id
                LIMIT ?
                """,
                (rs, rowNum) -> new DeploymentEvidence(
                        rs.getString("evidence_id"),
                        rs.getString("service_name"),
                        rs.getString("version"),
                        rs.getObject(
                                "deployed_at",
                                OffsetDateTime.class
                        ).toInstant(),
                        rs.getString("status"),
                        rs.getString("change_summary")
                ),
                serviceName,
                Timestamp.from(from),
                Timestamp.from(to),
                limit
        );
    }
}
