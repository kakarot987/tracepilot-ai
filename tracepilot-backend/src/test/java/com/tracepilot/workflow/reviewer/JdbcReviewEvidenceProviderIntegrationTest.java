
package com.tracepilot.workflow.reviewer;

import com.tracepilot.investigation.domain.EvidenceType;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.investigation.service.InvestigationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_POSTGRES_TEST",
        matches = "true"
)
@Transactional
@Rollback
class JdbcReviewEvidenceProviderIntegrationTest {

    private static final String INCIDENT_ID = "INC-PAY-001";
    private static final String LOG_ID = "LOG-PAY-001";

    @Autowired
    private JdbcReviewEvidenceProvider provider;

    @Autowired
    private InvestigationService investigations;

    private UUID investigationId;

    @BeforeEach
    void setUp() {
        var run = investigations.start(INCIDENT_ID);
        investigationId = run.id();
    }

    private void recordLogAndComplete() {
        investigations.recordEvidence(
                investigationId,
                LOG_ID,
                EvidenceType.LOG
        );

        investigations.finish(
                investigationId,
                InvestigationStatus.COMPLETED,
                null
        );
    }

    @Test
    void loadsRecordedLogEvidenceFromCorrectIncident() {
        recordLogAndComplete();

        List<EvidenceExcerpt> excerpts = provider.load(
                investigationId,
                List.of(LOG_ID)
        );

        assertEquals(1, excerpts.size());

        EvidenceExcerpt excerpt = excerpts.get(0);

        assertEquals(LOG_ID, excerpt.evidenceId());
        assertEquals("LOG", excerpt.evidenceType());

        assertNotNull(excerpt.content());
        assertFalse(excerpt.content().isBlank());
        assertTrue(excerpt.content().length() <= 1200);
    }

    @Test
    void doesNotLoadUnknownEvidenceId() {
        recordLogAndComplete();

        var excerpts = provider.load(
                investigationId,
                List.of("LOG-UNKNOWN-999")
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void doesNotLoadUnrecordedEvidence() {
        investigations.finish(
                investigationId,
                InvestigationStatus.COMPLETED,
                null
        );

        var excerpts = provider.load(
                investigationId,
                List.of(LOG_ID)
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void doesNotLoadEvidenceFromRunningInvestigation() {
        investigations.recordEvidence(
                investigationId,
                LOG_ID,
                EvidenceType.LOG
        );

        var excerpts = provider.load(
                investigationId,
                List.of(LOG_ID)
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void doesNotLoadEvidenceFromAnotherInvestigation() {
        recordLogAndComplete();

        var excerpts = provider.load(
                UUID.randomUUID(),
                List.of(LOG_ID)
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void doesNotLoadLogBelongingToDifferentIncident() {
        investigations.recordEvidence(
                investigationId,
                "LOG-DB-001",
                EvidenceType.LOG
        );

        investigations.finish(
                investigationId,
                InvestigationStatus.COMPLETED,
                null
        );

        var excerpts = provider.load(
                investigationId,
                List.of("LOG-DB-001")
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void doesNotLoadEvidenceRecordedWithWrongType() {
        investigations.recordEvidence(
                investigationId,
                LOG_ID,
                EvidenceType.DEPLOYMENT
        );

        investigations.finish(
                investigationId,
                InvestigationStatus.COMPLETED,
                null
        );

        var excerpts = provider.load(
                investigationId,
                List.of(LOG_ID)
        );

        assertTrue(excerpts.isEmpty());
    }

    @Test
    void rejectsMoreThanTwelveEvidenceIds() {
        var ids = java.util.stream.IntStream.range(0, 13)
                .mapToObj(i -> "LOG-TEST-" + i)
                .toList();

        assertThrows(
                IllegalArgumentException.class,
                () -> provider.load(investigationId, ids)
        );
    }

    @Test
    void rejectsDuplicateEvidenceIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.load(
                        investigationId,
                        List.of(LOG_ID, LOG_ID)
                )
        );
    }

    @Test
    void rejectsEmptyEvidenceList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.load(
                        investigationId,
                        List.of()
                )
        );
    }

    @Test
    void rejectsNullInvestigationId() {
        assertThrows(
                NullPointerException.class,
                () -> provider.load(
                        null,
                        List.of(LOG_ID)
                )
        );
    }
}
