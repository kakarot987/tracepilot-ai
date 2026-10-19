
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.ServiceInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("postgres")
public class JdbcServiceInfoRepository
        implements ServiceInfoRepository {

    private final JdbcClient jdbc;

    public JdbcServiceInfoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ServiceInfo> findByServiceName(
            String serviceName) {

        if (serviceName == null
                || serviceName.isBlank()
                || serviceName.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid serviceName"
            );
        }

        return jdbc.sql("""
                SELECT service_name, team, environment,
                       runtime, database_dependency,
                       upstream_dependency
                FROM service_info
                WHERE service_name = :serviceName
                """)
                .param("serviceName", serviceName)
                .query((rs, rowNum) -> new ServiceInfo(
                        rs.getString("service_name"),
                        rs.getString("team"),
                        rs.getString("environment"),
                        rs.getString("runtime"),
                        rs.getString("database_dependency"),
                        rs.getString("upstream_dependency")
                ))
                .optional();
    }
}
