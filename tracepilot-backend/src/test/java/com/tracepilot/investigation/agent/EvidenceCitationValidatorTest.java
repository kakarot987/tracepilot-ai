
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceCitationValidatorTest {

    private final EvidenceCitationValidator validator =
            new EvidenceCitationValidator();

    private EvidenceReference evidence(String id) {
        return new EvidenceReference(
                UUID.randomUUID(),
                id,
                EvidenceType.LOG,
                Instant.now()
        );
    }

    @Test
    void acceptsCitationsFromPersistedEvidence() {
        var result = validator.validate(
                "Connection timeout observed [LOG-PAY-003]",
                List.of(evidence("LOG-PAY-003"))
        );

        assertTrue(result.valid());
    }

    @Test
    void rejectsInventedEvidenceId() {
        var result = validator.validate(
                "Observed [LOG-PAY-003] and [LOG-FAKE-999]",
                List.of(evidence("LOG-PAY-003"))
        );

        assertFalse(result.valid());
        assertTrue(
                result.invalidEvidenceIds()
                        .contains("LOG-FAKE-999")
        );
    }

    @Test
    void rejectsMissingCitations() {
        var result = validator.validate(
                "Database connections timed out",
                List.of(evidence("LOG-PAY-003"))
        );

        assertFalse(result.valid());
    }

    @Test
    void rejectsEvidenceFromAnotherRun() {
        var result = validator.validate(
                "Failure observed [LOG-DB-003]",
                List.of(evidence("LOG-PAY-003"))
        );

        assertFalse(result.valid());
    }

    @Test
    void rejectsEmptyEvidence() {
        var result = validator.validate(
                "Failure observed [LOG-PAY-003]",
                List.of()
        );

        assertFalse(result.valid());
    }
}
