
package com.tracepilot.llm;

import com.tracepilot.incident.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.config.additional-location=optional:file:./config/"
})
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_LIVE_OLLAMA",
        matches = "true"
)
class OllamaLiveIntegrationTest {

    @Autowired
    private IncidentModelGateway gateway;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void qwen3ProducesValidJsonContent() throws Exception {

        String content = chatClientBuilder.build()
                .prompt()
                .system("""
            You are a software incident triage assistant.
            Return a JSON object containing:
            severity and category.
            Do not include explanations.
            """)
                .user("""
            Analyze a payment service HTTP 504 timeout.
            Return severity HIGH and category DATABASE.
            """)
                .options(OllamaChatOptions.builder()
                        .format("json")
                        .disableThinking()
                     )
                .call()
                .content();

        assertNotNull(content);
        assertFalse(content.isBlank(),
                "Ollama returned empty content");

        var json = objectMapper.readTree(content);

        assertTrue(json.isObject(), "Expected JSON object");
        assertTrue(json.has("severity"));
        assertTrue(json.has("category"));

        assertTrue(json.get("severity").isString());
        assertTrue(json.get("category").isString());
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
        assertTrue(Set.of(
                "LOW", "MEDIUM", "HIGH", "CRITICAL"
        ).contains(response.severity()));

        assertTrue(Set.of(
                "DATABASE",
                "NETWORK",
                "APPLICATION",
                "INFRASTRUCTURE",
                "EXTERNAL_DEPENDENCY",
                "UNKNOWN"
        ).contains(response.category()));

        assertFalse(response.hypotheses().isEmpty());
        assertTrue(response.hypotheses().size() <= 3);
        assertTrue(response.requiresInvestigation());

        response.hypotheses().forEach(h -> {
            assertNotNull(h);
            assertNotNull(h.cause());
            assertFalse(h.cause().isBlank());
            assertNotNull(h.reasoning());
            assertFalse(h.reasoning().isBlank());
        });

    }
}
