
package com.tracepilot.llm;

import com.tracepilot.incident.dto.IncidentAnalysisRequest;
import com.tracepilot.llm.exception.ModelTimeoutException;
import com.tracepilot.llm.exception.ModelUnavailableException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OllamaIncidentModelGatewayTest {

    private ChatClient.Builder builder;
    private ChatClient client;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private OllamaIncidentModelGateway gateway;

    private final IncidentAnalysisRequest incident =
            new IncidentAnalysisRequest(
                    "Payment timeout",
                    "HTTP 504 during traffic spike",
                    "payment-service"
            );

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        client = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString()))
                .thenReturn(requestSpec);
        when(requestSpec.user(anyString()))
                .thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        gateway = new OllamaIncidentModelGateway(builder);
    }

    @Test
    void connectionRefusedBecomesUnavailable() {
        when(requestSpec.call()).thenThrow(
                new ResourceAccessException(
                        "Connection refused",
                        new ConnectException("Connection refused")
                )
        );

        assertThrows(
                ModelUnavailableException.class,
                () -> gateway.invoke(incident)
        );
    }

    @Test
    void socketTimeoutBecomesModelTimeout() {
        when(requestSpec.call()).thenThrow(
                new ResourceAccessException(
                        "Read timed out",
                        new SocketTimeoutException("Read timed out")
                )
        );

        assertThrows(
                ModelTimeoutException.class,
                () -> gateway.invoke(incident)
        );
    }

    @Test
    void genericResourceFailureBecomesUnavailable() {
        when(requestSpec.call()).thenThrow(
                new ResourceAccessException("I/O failure")
        );

        assertThrows(
                ModelUnavailableException.class,
                () -> gateway.invoke(incident)
        );
    }

    @Test
    void unexpectedProgrammingErrorIsNotMisclassified() {
        when(requestSpec.call()).thenThrow(
                new IllegalArgumentException("Programming error")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> gateway.invoke(incident)
        );
    }
}
