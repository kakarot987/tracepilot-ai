
package com.tracepilot.workflow.reviewer;

import com.tracepilot.investigation.agent.BoundedModelRunner;
import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.workflow.domain.Hypothesis;
import com.tracepilot.workflow.rootcause.RootCauseResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewerAgentTest {

    private BoundedModelRunner runner;
    private ReviewerAgent reviewer;

    @BeforeEach
    void setUp() {
        runner = mock(BoundedModelRunner.class);
        reviewer = new ReviewerAgent(
                runner,
                new JsonMapper()
        );
    }

    private RootCauseResult supportedRootCause() {
        return new RootCauseResult(
                List.of(new Hypothesis(
                        "Database connection pool exhaustion",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of(),
                true
        );
    }

    private List<EvidenceExcerpt> operationalEvidence() {
        return List.of(
                new EvidenceExcerpt(
                        "LOG-DB-001",
                        "LOG",
                        "Active database connections reached " +
                                "the configured connection pool limit."
                )
        );
    }

    private void mockResponse(String json) {
        when(runner.run(
                anyString(),
                anyString(),
                eq(List.of()),
                any(InvestigationBudget.class),
                eq(true),
                eq(true)
        )).thenReturn(json);
    }

    @Test
    void acceptsSupportedOperationalHypothesis() {
        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["LOG-DB-001"],
                    "explanation":
                      "The log confirms connection pool saturation."
                  }],
                  "unresolvedQuestions": [],
                  "rationale":
                    "The hypothesis is supported by current logs."
                }
                """);

        ReviewerResult result = reviewer.review(
                supportedRootCause(),
                operationalEvidence()
        );

        assertEquals(ReviewDecision.ACCEPT,
                result.decision());
        assertEquals(1, result.claimReviews().size());
        assertTrue(result.claimReviews().get(0).supported());

        verify(runner).run(
                anyString(),
                anyString(),
                eq(List.of()),
                any(InvestigationBudget.class),
                eq(true),
                eq(true)
        );
    }

    @Test
    void rejectsFabricatedEvidenceCitation() {
        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["LOG-FAKE-999"],
                    "explanation": "Supposed supporting evidence."
                  }],
                  "unresolvedQuestions": [],
                  "rationale": "Evidence supports the hypothesis."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        supportedRootCause(),
                        operationalEvidence()
                )
        );
    }

    @Test
    void rejectsSkippedHypothesis() {
        var rootCause = new RootCauseResult(
                List.of(
                        new Hypothesis(
                                "Database pool exhaustion",
                                List.of("LOG-DB-001"),
                                List.of(),
                                "MEDIUM"
                        ),
                        new Hypothesis(
                                "Slow transactions",
                                List.of("LOG-DB-002"),
                                List.of(),
                                "LOW"
                        )
                ),
                List.of(),
                true
        );

        var evidence = List.of(
                new EvidenceExcerpt(
                        "LOG-DB-001",
                        "LOG",
                        "Connection pool at capacity."
                ),
                new EvidenceExcerpt(
                        "LOG-DB-002",
                        "LOG",
                        "Long-running transactions detected."
                )
        );

        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["LOG-DB-001"],
                    "explanation": "Pool saturation was observed."
                  }],
                  "unresolvedQuestions": [],
                  "rationale": "Investigation complete."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(rootCause, evidence)
        );
    }

    @Test
    void rejectsDuplicateHypothesisReview() {
        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [
                    {
                      "hypothesisIndex": 0,
                      "supported": true,
                      "evidenceIds": ["LOG-DB-001"],
                      "explanation": "First review."
                    },
                    {
                      "hypothesisIndex": 0,
                      "supported": true,
                      "evidenceIds": ["LOG-DB-001"],
                      "explanation": "Duplicate review."
                    }
                  ],
                  "unresolvedQuestions": [],
                  "rationale": "Approved."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        supportedRootCause(),
                        operationalEvidence()
                )
        );
    }

    @Test
    void rejectsHistoricalOnlyEvidenceForAccept() {
        var rootCause = new RootCauseResult(
                List.of(new Hypothesis(
                        "Connection pool exhaustion",
                        List.of("RB-002-v1-c000"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of(),
                true
        );

        var historicalEvidence = List.of(
                new EvidenceExcerpt(
                        "RB-002-v1-c000",
                        "RUNBOOK_CHUNK",
                        "Connection pool exhaustion can cause " +
                                "request timeouts."
                )
        );

        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["RB-002-v1-c000"],
                    "explanation":
                      "The runbook describes this failure mode."
                  }],
                  "unresolvedQuestions": [],
                  "rationale": "The runbook matches the hypothesis."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        rootCause,
                        historicalEvidence
                )
        );
    }

    @Test
    void rejectsAcceptWhenEvidenceExcerptIsMissing() {
        var rootCause = new RootCauseResult(
                List.of(new Hypothesis(
                        "Database connection exhaustion",
                        List.of("LOG-DB-001", "LOG-DB-002"),
                        List.of(),
                        "MEDIUM"
                )),
                List.of(),
                true
        );

        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["LOG-DB-001"],
                    "explanation": "One log supports saturation."
                  }],
                  "unresolvedQuestions": [],
                  "rationale": "Approved."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        rootCause,
                        operationalEvidence()
                )
        );
    }

    @Test
    void rejectsAcceptWhenRootCauseSaysInsufficient() {
        var rootCause = new RootCauseResult(
                List.of(new Hypothesis(
                        "Possible connection pool exhaustion",
                        List.of("LOG-DB-001"),
                        List.of(),
                        "LOW"
                )),
                List.of("What caused saturation?"),
                false
        );

        mockResponse("""
                {
                  "decision": "ACCEPT",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": true,
                    "evidenceIds": ["LOG-DB-001"],
                    "explanation": "Saturation was observed."
                  }],
                  "unresolvedQuestions": [],
                  "rationale": "Approved."
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        rootCause,
                        operationalEvidence()
                )
        );
    }

    @Test
    void returnsFollowUpWhenEvidenceIsIncomplete() {
        mockResponse("""
                {
                  "decision": "FOLLOW_UP",
                  "claimReviews": [{
                    "hypothesisIndex": 0,
                    "supported": false,
                    "evidenceIds": ["LOG-DB-001"],
                    "explanation":
                      "Saturation is observed but cause is unclear."
                  }],
                  "unresolvedQuestions": [
                    "Which transactions held connections?"
                  ],
                  "rationale":
                    "Additional operational evidence is needed."
                }
                """);

        ReviewerResult result = reviewer.review(
                supportedRootCause(),
                operationalEvidence()
        );

        assertEquals(ReviewDecision.FOLLOW_UP,
                result.decision());
        assertEquals(1, result.unresolvedQuestions().size());
    }

    @Test
    void rejectsMalformedJson() {
        mockResponse("The investigation looks correct.");

        assertThrows(
                IllegalArgumentException.class,
                () -> reviewer.review(
                        supportedRootCause(),
                        operationalEvidence()
                )
        );
    }
}
