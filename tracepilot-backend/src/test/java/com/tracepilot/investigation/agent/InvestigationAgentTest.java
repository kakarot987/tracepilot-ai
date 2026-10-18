
package com.tracepilot.investigation.agent;

import com.tracepilot.incident.repository.IncidentRepository;
import com.tracepilot.investigation.service.InvestigationService;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationAgentTest {

    @Test
    void rejectsUnknownIncidentBeforeStartingRun() {

        var builder = mock(ChatClient.Builder.class);
        var client = mock(ChatClient.class);

        when(builder.build()).thenReturn(client);

        var incidents = mock(IncidentRepository.class);
        var investigations = mock(InvestigationService.class);
        var tools = mock(InvestigationToolCallbackFactory.class);

        when(incidents.findById("INC-UNKNOWN"))
                .thenReturn(Optional.empty());

        var agent = new InvestigationAgent(
                builder,
                incidents,
                investigations,
                tools
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> agent.investigate("INC-UNKNOWN")
        );

        verifyNoInteractions(investigations, tools);
        verifyNoInteractions(client);
    }
}
