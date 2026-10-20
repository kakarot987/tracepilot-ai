
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

            // Only current-incident log evidence is
            // supported in this initial implementation.
            //
            // The JOINs enforce:
            // 1. Evidence was recorded by this investigation.
            // 2. Evidence type is LOG.
            // 3. Log belongs to the investigation's incident.
            var excerpt = jdbc.sql("""
                    SELECT
                        le.evidence_id,
                        le.level,
                        le.message,
                        le.service_name,
                        le.occurred_at
                    FROM investigation_runs ir
                    JOIN investigation_evidence ie
                      ON ie.investigation_id = ir.id
                    JOIN log_evidence le
                      ON le.evidence_id = ie.evidence_id
                     AND le.incident_id = ir.incident_id
                    WHERE ir.id = :investigationId
                      AND ie.evidence_id = :evidenceId
                      AND ie.evidence_type = 'LOG'
                      AND ir.status = 'COMPLETED'
                    """)
                    .param("investigationId", investigationRunId)
                    .param("evidenceId", evidenceId)
                    .query((rs, row) ->
                            new EvidenceExcerpt(
                                    rs.getString("evidence_id"),
                                    "LOG",
                                    truncate(
                                            "Level: " +
                                                    rs.getString("level") +
                                                    "; Service: " +
                                                    rs.getString("service_name") +
                                                    "; Time: " +
                                                    rs.getObject(
                                                            "occurred_at") +
                                                    "; Message: " +
                                                    rs.getString("message")
                                    )
                            )
                    )
                    .optional();

            // Unsupported or unresolved IDs are omitted.
            // The Reviewer may not approve hypotheses
            // whose cited material is unavailable.
            excerpt.ifPresent(excerpts::add);
        }

        return List.copyOf(excerpts);
    }

    private String truncate(String value) {
        return value.length() <= MAX_CONTENT_LENGTH
                ? value
                : value.substring(0, MAX_CONTENT_LENGTH);
    }
}
