
package com.tracepilot.investigation.api;

import com.tracepilot.investigation.agent.AgentExecution;
import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.investigation.domain.*;
import com.tracepilot.investigation.service.InvestigationService;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationControllerTest {

    private final InvestigationAgent agent =
            mock(InvestigationAgent.class);

    private final InvestigationService service =
            mock(InvestigationService.class);

    private final com.tracepilot.investigation.api.InvestigationController controller =
            new com.tracepilot.investigation.api.InvestigationController(agent, service);

    @Test
    void startsInvestigationForRequestedIncident() {
        UUID runId = UUID.randomUUID();

        when(agent.investigate("INC-PAY-001"))
                .thenReturn(new AgentExecution(
                        runId,
                        InvestigationStatus.COMPLETED,
                        "Analysis [LOG-PAY-001]",
                        3
                ));

        var response =
                controller.investigate("INC-PAY-001");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(runId, response.getBody().runId());

        verify(agent).investigate("INC-PAY-001");
    }

    @Test
    void retrievesPersistedRun() {
        UUID id = UUID.randomUUID();

        var run = new InvestigationRun(
                id,
                "INC-PAY-001",
                InvestigationStatus.TIMED_OUT,
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:02:00Z"),
                "Investigation deadline exceeded"
        );

        when(service.get(id)).thenReturn(run);

        assertEquals(run, controller.get(id));
        verify(service).get(id);
    }

    @Test
    void retrievesEvidenceForRun() {
        UUID id = UUID.randomUUID();

        var evidence = List.of(
                new EvidenceReference(
                        id,
                        "LOG-PAY-001",
                        EvidenceType.LOG,
                        Instant.parse("2026-10-01T10:00:00Z")
                )
        );

        when(service.evidence(id)).thenReturn(evidence);

        assertEquals(evidence, controller.evidence(id));
        verify(service).evidence(id);
    }
}
