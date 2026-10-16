
package com.tracepilot.llm;

import com.tracepilot.incident.dto.*;
import com.tracepilot.llm.config.LlmReliabilityProperties;
import com.tracepilot.llm.exception.*;

import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResilientIncidentModelGatewayTest {

    private IncidentModelInvocation invocation;
    private ExecutorService executor;
    private ResilientIncidentModelGateway gateway;

    private final IncidentAnalysisRequest request =
            new IncidentAnalysisRequest(
                    "Payment failure",
                    "HTTP 504 errors",
                    "payment-service"
            );

    private final IncidentAnalysisResponse result =
            new IncidentAnalysisResponse(
                    "HIGH",
                    "DATABASE",
                    List.of(new IncidentHypothesis(
                            "Connection exhaustion",
                            "Connections may be saturated"
                    )),
                    true
            );

    @BeforeEach
    void setup() {
        invocation = mock(IncidentModelInvocation.class);
        executor = Executors.newSingleThreadExecutor();

        gateway = new ResilientIncidentModelGateway(
                invocation,
                new LlmReliabilityProperties(
                        1,
                        Duration.ofMillis(10),
                        Duration.ofSeconds(2)
                ),
                executor
        );
    }

    @AfterEach
    void cleanup() {
        executor.shutdownNow();
    }

    @Test
    void returnsSuccessfulResult() {
        when(invocation.invoke(request)).thenReturn(result);

        assertEquals(result, gateway.analyze(request));
        verify(invocation, times(1)).invoke(request);
    }

    @Test
    void retriesOneTransientFailure() {
        when(invocation.invoke(request))
                .thenThrow(new ModelUnavailableException(
                        "Connection refused", new RuntimeException()
                ))
                .thenReturn(result);

        assertEquals(result, gateway.analyze(request));
        verify(invocation, times(2)).invoke(request);
    }

    @Test
    void stopsAfterRetryBudget() {
        when(invocation.invoke(request))
                .thenThrow(new ModelUnavailableException(
                        "Unavailable", new RuntimeException()
                ));

        assertThrows(
                ModelUnavailableException.class,
                () -> gateway.analyze(request)
        );

        verify(invocation, times(2)).invoke(request);
    }

    @Test
    void doesNotRetryPermanentOutputFailure() {
        when(invocation.invoke(request))
                .thenThrow(new ModelOutputException(
                        "Invalid JSON"
                ));

        assertThrows(
                ModelOutputException.class,
                () -> gateway.analyze(request)
        );

        verify(invocation, times(1)).invoke(request);
    }

    @Test
    void enforcesOverallDeadline() {
        var shortDeadlineGateway =
                new ResilientIncidentModelGateway(
                        invocation,
                        new LlmReliabilityProperties(
                                0,
                                Duration.ofMillis(10),
                                Duration.ofMillis(100)
                        ),
                        executor
                );

        when(invocation.invoke(request)).thenAnswer(inv -> {
            Thread.sleep(2000);
            return result;
        });

        assertThrows(
                ModelTimeoutException.class,
                () -> shortDeadlineGateway.analyze(request)
        );
    }
}
