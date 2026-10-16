
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.Incident;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("postgres")
public class JdbcIncidentRepository implements IncidentRepository {

    private final JdbcClient jdbc;

    private static final RowMapper<Incident> ROW_MAPPER =
            (rs, rowNum) -> new Incident(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("service_name"),
                    rs.getObject(
                            "occurred_at",
                            java.time.OffsetDateTime.class
                    ).toInstant()
            );

    public JdbcIncidentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Incident> findAll() {
        return jdbc.sql("""
                SELECT id, title, description,
                       service_name, occurred_at
                FROM incidents
                ORDER BY occurred_at, id
                LIMIT 100
                """)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<Incident> findById(String incidentId) {
        if (incidentId == null || incidentId.isBlank()) {
            return Optional.empty();
        }

        return jdbc.sql("""
                SELECT id, title, description,
                       service_name, occurred_at
                FROM incidents
                WHERE id = :incidentId
                """)
                .param("incidentId", incidentId)
                .query(ROW_MAPPER)
                .optional();
    }
}
