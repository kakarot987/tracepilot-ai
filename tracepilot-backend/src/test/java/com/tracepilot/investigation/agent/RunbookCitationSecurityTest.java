
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.EvidenceReference;
import com.tracepilot.investigation.domain.EvidenceType;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RunbookCitationSecurityTest {

    private final EvidenceCitationValidator validator =
            new EvidenceCitationValidator();

    private final UUID runId = UUID.randomUUID();

    private EvidenceReference evidence(
            String id, EvidenceType type) {

        return new EvidenceReference(
                runId, id, type, Instant.now()
        );
    }

    @Test
    void acceptsPersistedRunbookCitation() {

        var result = validator.validate(
                "Review pool saturation: RB-002-v1-c000",
                List.of(evidence(
                        "RB-002-v1-c000",
                        EvidenceType.RUNBOOK_CHUNK
                ))
        );

        assertTrue(result.valid());
    }

    @Test
    void rejectsUnrecordedRunbookCitation() {

        var result = validator.validate(
                "See RB-999-v1-c000",
                List.of(evidence(
                        "RB-002-v1-c000",
                        EvidenceType.RUNBOOK_CHUNK
                ))
        );

        assertFalse(result.valid());
        assertTrue(result.invalidEvidenceIds()
                .contains("RB-999-v1-c000"));
    }

    @Test
    void rejectsMixedValidAndInvalidCitations() {

        var result = validator.validate(
                "Observed LOG-DB-003. Also see PM-999-v1-c000.",
                List.of(evidence(
                        "LOG-DB-003", EvidenceType.LOG
                ))
        );

        assertFalse(result.valid());
    }

    @Test
    void acceptsPersistedPostmortemCitation() {

        var result = validator.validate(
                "Historical context: PM-002-v1-c000",
                List.of(evidence(
                        "PM-002-v1-c000",
                        EvidenceType.RUNBOOK_CHUNK
                ))
        );

        assertTrue(result.valid());
    }

    @Test
    void rejectsNoCitations() {

        var result = validator.validate(
                "The database is definitely overloaded.",
                List.of(evidence(
                        "RB-002-v1-c000",
                        EvidenceType.RUNBOOK_CHUNK
                ))
        );

        assertFalse(result.valid());
    }
}
