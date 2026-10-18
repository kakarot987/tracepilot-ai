
package com.tracepilot.investigation.repository;

import com.tracepilot.investigation.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRunRepository {

    boolean insert(InvestigationRun run);

    Optional<InvestigationRun> findById(UUID id);

    boolean finish(
            UUID id,
            InvestigationStatus status,
            String failureReason
    );

    void addEvidence(
            UUID investigationId,
            String evidenceId,
            EvidenceType type
    );

    List<EvidenceReference> findEvidence(UUID investigationId);

    boolean lockIfRunning(UUID id);
}
