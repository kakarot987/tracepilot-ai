
package com.tracepilot.workflow.rootcause;

import com.tracepilot.investigation.agent.BoundedModelRunner;
import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.workflow.investigator.InvestigatorStepResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RootCauseAgentTest {

    private BoundedModelRunner runner;
    private RootCauseAgent agent;

    @BeforeEach
    void setUp() {
        runner = mock(BoundedModelRunner.class);
        agent = new RootCauseAgent(
                runner, new JsonMapper());
    }

    private InvestigatorStepResult investigation() {
        return new InvestigatorStepResult(
                UUID.randomUUID(),
                InvestigationStatus.COMPLETED,
                "Current logs indicate connection saturation.",
                List.of("LOG-DB-001", "LOG-DB-002"),
                2
        );
    }

    private void response(String json) {
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
    void acceptsGroundedHypotheses() {
        response("""
                {
                  "hypotheses": [{
                    "description": "Connection pool exhaustion",
                    "supportingEvidenceIds": ["LOG-DB-001"],
                    "contradictingEvidenceIds": [],
                    "confidence": "MEDIUM"
                  }],
                  "openQuestions": [
                    "Which operation held connections?"
                  ],
                  "sufficientEvidence": false
                }
                """);

        var result = agent.analyze(investigation());

        assertEquals(1, result.hypotheses().size());
        assertFalse(result.sufficientEvidence());
        assertEquals(
                List.of("LOG-DB-001"),
                result.hypotheses().get(0)
                        .supportingEvidenceIds());

        verify(runner).run(
                anyString(),
                anyString(),
                eq(List.of()),
                any(InvestigationBudget.class),
                eq(true),
                eq(true));
    }

    @Test
    void rejectsFabricatedCitation() {
        response("""
                {
                  "hypotheses": [{
                    "description": "Unverified cause",
                    "supportingEvidenceIds": ["LOG-FAKE-999"],
                    "contradictingEvidenceIds": [],
                    "confidence": "HIGH"
                  }],
                  "openQuestions": [],
                  "sufficientEvidence": true
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.analyze(investigation()));
    }

    @Test
    void rejectsUnsupportedSufficiency() {
        response("""
                {
                  "hypotheses": [{
                    "description": "Possible upstream failure",
                    "supportingEvidenceIds": [],
                    "contradictingEvidenceIds": [],
                    "confidence": "LOW"
                  }],
                  "openQuestions": [],
                  "sufficientEvidence": true
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.analyze(investigation()));
    }

    @Test
    void rejectsSufficiencyWhenOnlyOneOfTwoHypothesesIsSupported() {
        response("""
                {
                  "hypotheses": [
                    {
                      "description": "Pool exhaustion",
                      "supportingEvidenceIds": ["LOG-DB-001"],
                      "contradictingEvidenceIds": [],
                      "confidence": "MEDIUM"
                    },
                    {
                      "description": "Unproven connection leak",
                      "supportingEvidenceIds": [],
                      "contradictingEvidenceIds": [],
                      "confidence": "LOW"
                    }
                  ],
                  "openQuestions": [],
                  "sufficientEvidence": true
                }
                """);
        var exception = assertThrows(IllegalArgumentException.class,
                () -> agent.analyze(investigation()));
        assertEquals(
                "Cannot claim sufficient evidence with unsupported hypotheses",
                exception.getMessage());
    }

    @Test
    void allowsMultipleCompetingHypothesesWithExplicitUncertainty() {
        response("""
                {
                  "hypotheses": [
                    {
                      "description": "Pool exhaustion",
                      "supportingEvidenceIds": ["LOG-DB-001"],
                      "contradictingEvidenceIds": [],
                      "confidence": "MEDIUM"
                    },
                    {
                      "description": "Possible slow queries",
                      "supportingEvidenceIds": [],
                      "contradictingEvidenceIds": [],
                      "confidence": "LOW"
                    }
                  ],
                  "openQuestions": ["Was request traffic higher?"],
                  "sufficientEvidence": false
                }
                """);
        var result = agent.analyze(investigation());
        assertFalse(result.sufficientEvidence());
        assertEquals(2, result.hypotheses().size());
    }

    @Test
    void rejectsIncompleteInvestigation() {
        var incomplete = new InvestigatorStepResult(
                UUID.randomUUID(),
                InvestigationStatus.INSUFFICIENT_EVIDENCE,
                "",
                List.of(),
                1
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.analyze(incomplete));

        verifyNoInteractions(runner);
    }

    @Test
    void rejectsMalformedJson() {
        response("Root cause: database timeout");

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.analyze(investigation()));
    }
}
