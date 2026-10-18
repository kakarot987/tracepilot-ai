
package com.tracepilot.investigation.agent;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.investigation.domain.*;
import com.tracepilot.investigation.service.*;

import com.tracepilot.investigation.service.InvestigationService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationAgentFinalizationTest {

    private final InvestigationService service =
            mock(InvestigationService.class);

    private InvestigationAgent agent() {
        var builder = mock(ChatClient.Builder.class);
        when(builder.build())
                .thenReturn(mock(ChatClient.class));

        return new InvestigationAgent(
                builder,
                mock(IncidentRepository.class),
                service,
                mock(InvestigationToolCallbackFactory.class),
                new EvidenceCitationValidator()
        );
    }

    private AgentExecution finalizeWith(
            InvestigationAgent agent,
            UUID runId,
            InvestigationStatus status,
            String draft) {

        return ReflectionTestUtils.invokeMethod(
                agent,
                "finalizeRun",
                runId,
                status,
                draft,
                null,
                new InvestigationBudget(
                        8,
                        Duration.ofSeconds(90)
                )
        );
    }

    @Test
    void successfulCompletionReturnsDraft() {
        UUID runId = UUID.randomUUID();

        var result = finalizeWith(
                agent(),
                runId,
                InvestigationStatus.COMPLETED,
                "Supported analysis [LOG-PAY-001]"
        );

        assertEquals(
                InvestigationStatus.COMPLETED,
                result.status()
        );

        assertTrue(
                result.draftAnalysis()
                        .contains("LOG-PAY-001")
        );

        verify(service).finish(
                runId,
                InvestigationStatus.COMPLETED,
                null
        );
    }

    @Test
    void lateResultCannotOverwriteTimedOutRun() {
        UUID runId = UUID.randomUUID();

        doThrow(new InvestigationConflictException(
                "Run already terminal"
        )).when(service).finish(
                runId,
                InvestigationStatus.COMPLETED,
                null
        );

        when(service.get(runId)).thenReturn(
                new InvestigationRun(
                        runId,
                        "INC-PAY-001",
                        InvestigationStatus.TIMED_OUT,
                        Instant.now().minusSeconds(180),
                        Instant.now(),
                        "Recovery deadline exceeded"
                )
        );

        var result = finalizeWith(
                agent(),
                runId,
                InvestigationStatus.COMPLETED,
                "Late model analysis [LOG-PAY-001]"
        );

        assertEquals(
                InvestigationStatus.TIMED_OUT,
                result.status()
        );

        assertEquals("", result.draftAnalysis());
        verify(service).get(runId);
    }
}
