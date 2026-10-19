
package com.tracepilot.investigation.service;

import com.tracepilot.investigation.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
@Transactional
@Rollback
class InvestigationServiceIntegrationTest {

    @Autowired
    private com.tracepilot.investigation.service.InvestigationService service;

    @Test
    void createsAndPersistsInvestigation() {
        var created = service.start("INC-PAY-001");
        var loaded = service.get(created.id());

        assertEquals(created.id(), loaded.id());
        assertEquals("INC-PAY-001", loaded.incidentId());
        assertEquals(InvestigationStatus.RUNNING, loaded.status());
        assertNull(loaded.completedAt());
    }

    @Test
    void rejectsDuplicateActiveInvestigation() {
        service.start("INC-PAY-001");

        assertThrows(
                InvestigationConflictException.class,
                () -> service.start("INC-PAY-001")
        );
    }

    @Test
    void allowsAnotherRunAfterCompletion() {
        var first = service.start("INC-PAY-001");

        service.finish(
                first.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        var second = service.start("INC-PAY-001");

        assertNotEquals(first.id(), second.id());
        assertEquals(
                InvestigationStatus.RUNNING,
                second.status()
        );
    }

    @Test
    void recordsEvidenceIdempotently() {
        var run = service.start("INC-PAY-001");

        service.recordEvidence(
                run.id(), "LOG-PAY-001", EvidenceType.LOG
        );
        service.recordEvidence(
                run.id(), "LOG-PAY-001", EvidenceType.LOG
        );

        var references = service.evidence(run.id());

        assertEquals(1, references.size());
        assertEquals(
                "LOG-PAY-001",
                references.get(0).evidenceId()
        );
    }

    @Test
    void rejectsEvidenceAfterCompletion() {
        var run = service.start("INC-PAY-001");

        service.finish(
                run.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        assertThrows(
                InvestigationConflictException.class,
                () -> service.recordEvidence(
                        run.id(), "LOG-PAY-001", EvidenceType.LOG
                )
        );
    }

    @Test
    void rejectsRepeatedCompletion() {
        var run = service.start("INC-PAY-001");

        service.finish(
                run.id(),
                InvestigationStatus.COMPLETED,
                null
        );

        assertThrows(
                InvestigationConflictException.class,
                () -> service.finish(
                        run.id(),
                        InvestigationStatus.FAILED,
                        "Late error"
                )
        );
    }

    @Test
    void rejectsUnknownIncident() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.start("INC-UNKNOWN")
        );
    }
}
