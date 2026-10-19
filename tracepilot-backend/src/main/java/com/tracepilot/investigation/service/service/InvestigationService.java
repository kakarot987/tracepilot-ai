
package com.tracepilot.investigation.service;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.investigation.domain.*;
import com.tracepilot.investigation.repository.InvestigationRunRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class InvestigationService {

    private final IncidentRepository incidents;
    private final InvestigationRunRepository runs;
    private final Clock clock = Clock.systemUTC();

    public InvestigationService(
            IncidentRepository incidents,
            InvestigationRunRepository runs) {
        this.incidents = incidents;
        this.runs = runs;
    }

    @Transactional
    public InvestigationRun start(String incidentId) {
        if (incidentId == null
                || incidentId.isBlank()
                || incidentId.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid incident ID"
            );
        }

        if (incidents.findById(incidentId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Incident does not exist"
            );
        }

        InvestigationRun run = new InvestigationRun(
                UUID.randomUUID(),
                incidentId,
                InvestigationStatus.RUNNING,
                Instant.now(clock),
                null,
                null
        );

        if (!runs.insert(run)) {
            throw new InvestigationConflictException(
                    "An investigation is already running for this incident"
            );
        }

        return run;
    }

    @Transactional
    public InvestigationRun finish(
            UUID runId,
            InvestigationStatus status,
            String failureReason) {

        Objects.requireNonNull(runId);

        if (status == null || !status.isTerminal()) {
            throw new IllegalArgumentException(
                    "A terminal status is required"
            );
        }

        if (failureReason != null
                && failureReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Failure reason is too long"
            );
        }

        // Only store sanitized operational reasons here,
        // never stack traces, raw logs or secrets.
        if (!runs.finish(runId, status, failureReason)) {
            throw new InvestigationConflictException(
                    "Run not found or already terminal"
            );
        }

        return runs.findById(runId).orElseThrow();
    }

    @Transactional
    public void recordEvidence(
            UUID runId,
            String evidenceId,
            EvidenceType type) {

        Objects.requireNonNull(runId);
        Objects.requireNonNull(type);

        if (evidenceId == null
                || evidenceId.isBlank()
                || evidenceId.length() > 100) {
            throw new IllegalArgumentException(
                    "Invalid evidence ID"
            );
        }

        if (!runs.lockIfRunning(runId)) {
            throw new InvestigationConflictException(
                    "Evidence can only be recorded on a running investigation"
            );
        }

        runs.addEvidence(runId, evidenceId, type);
    }

    @Transactional(readOnly = true)
    public InvestigationRun get(UUID id) {
        return runs.findById(Objects.requireNonNull(id))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Investigation run not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<EvidenceReference> evidence(UUID id) {
        get(id);
        return runs.findEvidence(id);
    }
}
