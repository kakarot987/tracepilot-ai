
package com.tracepilot.llm;

import com.tracepilot.incident.dto.IncidentAnalysisRequest;
import com.tracepilot.incident.dto.IncidentAnalysisResponse;
import com.tracepilot.llm.config.LlmReliabilityProperties;
import com.tracepilot.llm.exception.ModelOutputException;
import com.tracepilot.llm.exception.ModelTimeoutException;
import com.tracepilot.llm.exception.ModelUnavailableException;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ResilientIncidentModelGateway
        implements IncidentModelGateway {

    private final IncidentModelInvocation invocation;
    private final LlmReliabilityProperties properties;
    private final ExecutorService executor;

    public ResilientIncidentModelGateway(
            IncidentModelInvocation invocation,
            LlmReliabilityProperties properties,
            ExecutorService llmExecutor) {
        this.invocation = invocation;
        this.properties = properties;
        this.executor = llmExecutor;
    }

    @Override
    public IncidentAnalysisResponse analyze(
            IncidentAnalysisRequest request) {

        Future<IncidentAnalysisResponse> future;

        try {
            future = executor.submit(
                    () -> invokeWithRetry(request)
            );
        } catch (RejectedExecutionException ex) {
            throw new ModelUnavailableException(
                    "LLM request capacity exhausted", ex
            );
        }

        try {
            return future.get(
                    properties.requestDeadline().toMillis(),
                    TimeUnit.MILLISECONDS
            );

        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new ModelTimeoutException(
                    "LLM request deadline exceeded", ex
            );

        } catch (InterruptedException ex) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ModelTimeoutException(
                    "LLM request interrupted", ex
            );

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new IllegalStateException(
                    "Unexpected LLM execution failure", cause
            );
        }
    }

    private IncidentAnalysisResponse invokeWithRetry(
            IncidentAnalysisRequest request) {

        int maxAttempts =
                properties.maxTransientRetries() + 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return invocation.invoke(request);

            } catch (ModelOutputException ex) {
                // Permanent output/validation error.
                throw ex;

            } catch (ModelUnavailableException
                     | ModelTimeoutException ex) {

                if (attempt == maxAttempts) {
                    throw ex;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(
                            properties.retryBackoff().toMillis()
                    );
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new ModelTimeoutException(
                            "LLM retry interrupted", interrupted
                    );
                }
            }
        }

        throw new IllegalStateException(
                "Unreachable LLM retry state"
        );
    }
}
