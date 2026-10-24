
package com.tracepilot.workflow.reviewer;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
@Profile("postgres")
public class JdbcReviewEvidenceProvider
        implements ReviewEvidenceProvider {

    private static final int MAX_EXCERPTS = 12;
    private static final int MAX_CONTENT_LENGTH = 1200;

    private final JdbcClient jdbc;

    public JdbcReviewEvidenceProvider(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public List<EvidenceExcerpt> load(
            UUID investigationRunId,
            List<String> evidenceIds) {

        Objects.requireNonNull(investigationRunId);
        Objects.requireNonNull(evidenceIds);

        if (evidenceIds.isEmpty()
                || evidenceIds.size() > MAX_EXCERPTS) {
            throw new IllegalArgumentException(
                    "Expected 1 to 12 evidence IDs");
        }

        if (evidenceIds.stream().anyMatch(
                id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException(
                    "Invalid evidence ID");
        }

        var unique = new LinkedHashSet<>(evidenceIds);

        if (unique.size() != evidenceIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate evidence ID");
        }

        List<EvidenceExcerpt> excerpts =
                new ArrayList<>();


        for (String evidenceId : unique) {
            var scope = scope(investigationRunId, evidenceId);

            if (scope.isEmpty()) {
                continue;
            }

            java.util.Optional<EvidenceExcerpt> excerpt =
                    switch (scope.get().evidenceType()) {
                        case "LOG" ->
                                loadLog(scope.get(), evidenceId);
                        case "DEPLOYMENT" ->
                                loadDeployment(scope.get(), evidenceId);
                        case "SERVICE_INFO" ->
                                loadServiceInfo(scope.get(), evidenceId);
                        case "INCIDENT_HISTORY" ->
                                loadIncidentHistory(scope.get(), evidenceId);
                        case "RUNBOOK_CHUNK" ->
                                loadRunbookChunk(evidenceId);
                        default -> java.util.Optional.empty();
                    };

            excerpt.ifPresent(excerpts::add);
        }

        return List.copyOf(excerpts);
    }

    private String truncate(String value) {
        return value.length() <= MAX_CONTENT_LENGTH
                ? value
                : value.substring(0, MAX_CONTENT_LENGTH);
    }

    private record EvidenceScope(
            String incidentId,
            String serviceName,
            String evidenceType
    ) {}

    private java.util.Optional<EvidenceScope> scope(
            UUID investigationId,
            String evidenceId) {

        return jdbc.sql("""
            SELECT ir.incident_id,
                   i.service_name,
                   ie.evidence_type
            FROM investigation_runs ir
            JOIN incidents i
              ON i.id = ir.incident_id
            JOIN investigation_evidence ie
              ON ie.investigation_id = ir.id
            WHERE ir.id = :investigationId
              AND ir.status = 'COMPLETED'
              AND ie.evidence_id = :evidenceId
            """)
                .param("investigationId", investigationId)
                .param("evidenceId", evidenceId)
                .query((rs, row) -> new EvidenceScope(
                        rs.getString("incident_id"),
                        rs.getString("service_name"),
                        rs.getString("evidence_type")
                ))
                .optional();
    }

    private java.util.Optional<EvidenceExcerpt> loadDeployment(
            EvidenceScope scope,
            String evidenceId) {

        return jdbc.sql("""
            SELECT d.version, d.deployed_at,
                   d.status, d.change_summary
            FROM deployment_evidence d
            WHERE d.evidence_id = :evidenceId
              AND d.service_name = :serviceName
            """)
                .param("evidenceId", evidenceId)
                .param("serviceName", scope.serviceName())
                .query((rs, row) -> new EvidenceExcerpt(
                        evidenceId,
                        "DEPLOYMENT",
                        truncate("""
                            Version: %s
                            Deployed at: %s
                            Status: %s
                            Change: %s
                            """.formatted(
                                rs.getString("version"),
                                rs.getObject("deployed_at"),
                                rs.getString("status"),
                                rs.getString("change_summary")
                        ))
                ))
                .optional();
    }

    private java.util.Optional<EvidenceExcerpt> loadServiceInfo(
            EvidenceScope scope,
            String evidenceId) {

        return jdbc.sql("""
            SELECT service_name, team, environment,
                   runtime, database_dependency,
                   upstream_dependency
            FROM service_info
            WHERE service_name = :serviceName
              AND CONCAT('SVC-', service_name) = :evidenceId
            """)
                .param("serviceName", scope.serviceName())
                .param("evidenceId", evidenceId)
                .query((rs, row) -> new EvidenceExcerpt(
                        evidenceId,
                        "SERVICE_INFO",
                        truncate("""
                            Service: %s
                            Team: %s
                            Environment: %s
                            Runtime: %s
                            Database dependency: %s
                            Upstream dependency: %s
                            """.formatted(
                                rs.getString("service_name"),
                                rs.getString("team"),
                                rs.getString("environment"),
                                rs.getString("runtime"),
                                rs.getString("database_dependency"),
                                rs.getString("upstream_dependency")
                        ))
                ))
                .optional();
    }

    private java.util.Optional<EvidenceExcerpt> loadIncidentHistory(
            EvidenceScope scope,
            String evidenceId) {

        return jdbc.sql("""
            SELECT h.title, h.summary,
                   h.occurred_at, h.incident_id
            FROM incident_history h
            JOIN incidents current_incident
              ON current_incident.id = :incidentId
            WHERE h.evidence_id = :evidenceId
              AND h.service_name = :serviceName
              AND h.occurred_at < current_incident.occurred_at
            """)
                .param("incidentId", scope.incidentId())
                .param("evidenceId", evidenceId)
                .param("serviceName", scope.serviceName())
                .query((rs, row) -> new EvidenceExcerpt(
                        evidenceId,
                        "INCIDENT_HISTORY",
                        truncate("""
                            Historical incident: %s
                            Occurred at: %s
                            Title: %s
                            Summary: %s
                            """.formatted(
                                rs.getString("incident_id"),
                                rs.getObject("occurred_at"),
                                rs.getString("title"),
                                rs.getString("summary")
                        ))
                ))
                .optional();
    }

    private java.util.Optional<EvidenceExcerpt> loadRunbookChunk(
            String evidenceId) {

        return jdbc.sql("""
            SELECT d.title, d.document_type,
                   d.version, c.content
            FROM rag_chunks c
            JOIN rag_documents d
              ON d.document_id = c.document_id
            WHERE c.chunk_id = :evidenceId
            """)
                .param("evidenceId", evidenceId)
                .query((rs, row) -> new EvidenceExcerpt(
                        evidenceId,
                        "RUNBOOK_CHUNK",
                        truncate("""
                            Document: %s
                            Type: %s
                            Version: %s
                            Content: %s
                            """.formatted(
                                rs.getString("title"),
                                rs.getString("document_type"),
                                rs.getInt("version"),
                                rs.getString("content")
                        ))
                ))
                .optional();
    }

    private java.util.Optional<EvidenceExcerpt> loadLog(
            EvidenceScope scope,
            String evidenceId) {

        return jdbc.sql("""
            SELECT
                le.evidence_id,
                le.level,
                le.message,
                le.service_name,
                le.occurred_at
            FROM log_evidence le
            WHERE le.evidence_id = :evidenceId
              AND le.incident_id = :incidentId
              AND le.service_name = :serviceName
            """)
                .param("evidenceId", evidenceId)
                .param("incidentId", scope.incidentId())
                .param("serviceName", scope.serviceName())
                .query((rs, row) -> new EvidenceExcerpt(
                        rs.getString("evidence_id"),
                        "LOG",
                        truncate("""
                            Level: %s
                            Service: %s
                            Time: %s
                            Message: %s
                            """.formatted(
                                rs.getString("level"),
                                rs.getString("service_name"),
                                rs.getObject("occurred_at"),
                                rs.getString("message")
                        ))
                ))
                .optional();
    }

}
