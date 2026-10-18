
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.HistoricalIncident;
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
public class JdbcIncidentHistoryRepository
        implements IncidentHistoryRepository {

    private static final int MAX_ROWS = 20;

    private final JdbcTemplate jdbc;

    public JdbcIncidentHistoryRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(3);
    }

    @Override
    public List<HistoricalIncident> findHistory(
            String serviceName,
            Instant before,
            int limit) {

        if (serviceName == null
                || serviceName.isBlank()
                || serviceName.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid serviceName"
            );
        }

        if (before == null) {
            throw new IllegalArgumentException(
                    "before is required"
            );
        }

        if (limit < 1 || limit > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 20"
            );
        }

        return jdbc.query("""
                SELECT evidence_id,
                       incident_id,
                       service_name,
                       occurred_at,
                       title,
                       summary
                FROM incident_history
                WHERE service_name = ?
                  AND occurred_at < ?
                ORDER BY occurred_at DESC, evidence_id
                LIMIT ?
                """,
                (rs, rowNum) -> new HistoricalIncident(
                        rs.getString("evidence_id"),
                        rs.getString("incident_id"),
                        rs.getString("service_name"),
                        rs.getObject(
                                "occurred_at",
                                OffsetDateTime.class
                        ).toInstant(),
                        rs.getString("title"),
                        rs.getString("summary")
                ),
                serviceName,
                Timestamp.from(before),
                limit
        );
    }
}
