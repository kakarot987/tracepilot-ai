package com.tracepilot.workflow.reviewer;

import java.util.List;
import java.util.UUID;

public interface ReviewEvidenceProvider {

    List<EvidenceExcerpt> load(
            UUID investigationRunId,
            List<String> evidenceIds
    );
}