
package com.tracepilot.workflow.triage;

import com.tracepilot.incident.domain.Incident;
import com.tracepilot.investigation.agent.BoundedModelRunner;
import com.tracepilot.investigation.domain.InvestigationBudget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TriageAgentTest {

    private BoundedModelRunner runner;
    private TriageAgent agent;

    @BeforeEach
    void setUp() {
        runner = mock(BoundedModelRunner.class);
        agent = new TriageAgent(
                runner,
                new JsonMapper()
        );
    }

    private Incident incident() {
        return new Incident(
                "INC-DB-001",
                "Database connections exhausted",
                "Orders are experiencing connection timeouts",
                "order-service",
                Instant.parse("2026-10-19T10:00:00Z")
        );
    }

    @Test
    void parsesValidTriageResponse() {

        when(runner.run(
                anyString(),
                anyString(),
                anyList(),
                any(InvestigationBudget.class)
        )).thenReturn("""
                {
                  "severity": "HIGH",
                  "category": "DATABASE",
                  "investigationFocus":
                    "Inspect connection pool saturation",
                  "openQuestions": [
                    "Is the connection pool at capacity?",
                    "Are transactions taking too long?"
                  ]
                }
                """);

        TriageResult result = agent.triage(incident());

        assertEquals(TriageSeverity.HIGH,
                result.severity());

        assertEquals(TriageCategory.DATABASE,
                result.category());

        assertEquals(2, result.openQuestions().size());

        verify(runner).run(
                anyString(),
                anyString(),
                eq(List.of()),
                any(InvestigationBudget.class)
        );
    }

    @Test
    void rejectsMalformedJson() {

        when(runner.run(
                anyString(), anyString(),
                anyList(), any()
        )).thenReturn("This is not JSON");

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.triage(incident())
        );
    }

    @Test
    void rejectsInvalidSeverity() {

        when(runner.run(
                anyString(), anyString(),
                anyList(), any()
        )).thenReturn("""
                {
                  "severity": "SUPER_CRITICAL",
                  "category": "DATABASE",
                  "investigationFocus": "Inspect connections",
                  "openQuestions": ["What changed?"]
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.triage(incident())
        );
    }

    @Test
    void rejectsEmptyQuestions() {

        when(runner.run(
                anyString(), anyString(),
                anyList(), any()
        )).thenReturn("""
                {
                  "severity": "MEDIUM",
                  "category": "APPLICATION",
                  "investigationFocus": "Inspect request errors",
                  "openQuestions": []
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.triage(incident())
        );
    }
}
