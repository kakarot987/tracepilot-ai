
package com.tracepilot.telemetry.repository;

import com.tracepilot.telemetry.domain.LogEvidence;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@Profile("postgres")
public class JdbcLogEvidenceRepository
        implements LogEvidenceRepository {

    private static final int MAX_ROWS = 100;

    private static final RowMapper<LogEvidence> ROW_MAPPER =
            (rs, rowNum) -> new LogEvidence(
                    rs.getString("evidence_id"),
                    rs.getString("incident_id"),
                    rs.getString("service_name"),
                    rs.getObject(
                            "occurred_at",
                            OffsetDateTime.class
                    ).toInstant(),
                    rs.getString("level"),
                    rs.getString("message")
            );

    private final JdbcTemplate jdbc;

    public JdbcLogEvidenceRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);

        // JDBC query timeout in seconds.
        this.jdbc.setQueryTimeout(3);
    }

    @Override
    public List<LogEvidence> search(
            String incidentId,
            Instant from,
            Instant to,
            int limit) {

        if (incidentId == null || incidentId.isBlank()
                || incidentId.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid incidentId"
            );
        }

        if (from == null
                || to == null
                || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Invalid time window"
            );
        }

        if (limit < 1 || limit > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 100"
            );
        }

        return jdbc.query("""
                SELECT
                    evidence_id,
                    incident_id,
                    service_name,
                    occurred_at,
                    level,
                    message
                FROM log_evidence
                WHERE incident_id = ?
                  AND occurred_at >= ?
                  AND occurred_at <= ?
                ORDER BY occurred_at, evidence_id
                LIMIT ?
                """,
                ROW_MAPPER,
                incidentId,
                Timestamp.from(from),
                Timestamp.from(to),
                limit
        );
    }
}
