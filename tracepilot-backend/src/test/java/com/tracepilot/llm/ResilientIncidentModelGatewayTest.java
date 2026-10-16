
package com.tracepilot.llm;

import com.tracepilot.incident.dto.*;
import com.tracepilot.llm.config.LlmReliabilityProperties;
import com.tracepilot.llm.exception.*;

import org.junit.jupiter.api.*;

import java.net.SocketTimeoutException;
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

    @Test
    void retriesTimeoutOnceThenSucceeds() {
        when(invocation.invoke(request))
                .thenThrow(new ModelTimeoutException(
                        "Read timeout",
                        new SocketTimeoutException("Timed out")
                ))
                .thenReturn(result);

        assertEquals(result, gateway.analyze(request));

        verify(invocation, times(2)).invoke(request);
    }

    @Test
    void blockedInvocationDoesNotMakeNextRequestWaitForever()
            throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(invocation.invoke(request)).thenAnswer(call -> {
            started.countDown();

            // Simulate a dependency that ignores interruption.
            while (true) {
                try {
                    release.await();
                    return result;
                } catch (InterruptedException ignored) {
                    // Deliberately ignore cancellation.
                }
            }
        });

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

        try {
            assertThrows(
                    ModelTimeoutException.class,
                    () -> shortDeadlineGateway.analyze(request)
            );

            assertTrue(started.await(1, TimeUnit.SECONDS));

            long start = System.nanoTime();

            assertThrows(
                    ModelTimeoutException.class,
                    () -> shortDeadlineGateway.analyze(request)
            );

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start
            );

            assertTrue(elapsedMs < 1000,
                    "Second request exceeded expected wait");
        } finally {
            release.countDown();
        }
    }

    @Test
    void workerRecoversAfterTimedOutInvocationEnds()
            throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        var shortGateway = new ResilientIncidentModelGateway(
                invocation,
                new LlmReliabilityProperties(
                        0,
                        Duration.ofMillis(10),
                        Duration.ofMillis(100)
                ),
                executor
        );

        when(invocation.invoke(request))
                .thenAnswer(call -> {
                    started.countDown();

                    // Deliberately simulate an operation that
                    // ignores thread interruption.
                    boolean completed = false;

                    while (!completed) {
                        try {
                            release.await();
                            completed = true;
                        } catch (InterruptedException ignored) {
                            // Simulate a blocked dependency.
                        }
                    }

                    return result;
                })
                .thenReturn(result);

        try {
            assertThrows(
                    ModelTimeoutException.class,
                    () -> shortGateway.analyze(request)
            );

            assertTrue(started.await(1, TimeUnit.SECONDS));

            // The worker is still occupied even though
            // the Future has been cancelled.
            release.countDown();

            // Confirm the worker is available again.
            Future<?> probe = executor.submit(() -> {});

            assertDoesNotThrow(
                    () -> probe.get(2, TimeUnit.SECONDS)
            );

            assertEquals(result, shortGateway.analyze(request));
            verify(invocation, times(2)).invoke(request);

        } finally {
            release.countDown();
        }
    }

}
