
package com.tracepilot.llm;

import com.tracepilot.incident.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_LIVE_OLLAMA",
        matches = "true"
)
class OllamaLiveIntegrationTest {

    @Autowired
    private IncidentModelGateway gateway;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void qwen3ProducesNonEmptyJsonContent() {
        String content = chatClientBuilder.build()
                .prompt()
                .system("""
            You are a software incident triage assistant.
            Return only a JSON object.
            Do not include markdown.
            """)
                .user("""
            Analyze a payment service HTTP 504 timeout.
            Return fields severity and category.
            """).options(OllamaChatOptions.builder()
                        .disableThinking())
                .call()
                .content();

        assertNotNull(content);
        assertFalse(content.isBlank(),
                "Ollama returned empty answer content");
    }

    @Test
    void analyzesRealSyntheticIncident() {

        IncidentAnalysisResponse response =
                gateway.analyze(new IncidentAnalysisRequest(
                        "Payment service timeout",
                        "Payment API returns HTTP 504. "
                                + "Database connection pool is exhausted.",
                        "payment-service"
                ));

        assertNotNull(response);
        assertNotNull(response.severity());
        assertNotNull(response.category());
        assertFalse(response.hypotheses().isEmpty());
        assertTrue(response.requiresInvestigation());

        assertTrue(response.hypotheses().size() <= 3);
    }
}
