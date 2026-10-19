
package com.tracepilot.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_EMBEDDING_TEST",
        matches = "true"
)
class EmbeddingModelIntegrationTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void generatesValidEmbedding() {

        float[] vector = embeddingModel.embed(
                "Payment service JDBC connection pool exhausted"
        );

        assertNotNull(vector);
        assertTrue(vector.length > 0);

        assertTrue(
                Arrays.stream(toDoubleArray(vector))
                        .allMatch(Double::isFinite)
        );

        System.out.println(
                "Embedding dimension: " + vector.length
        );
    }

    @Test
    void repeatedEmbeddingHasSameDimension() {

        float[] first = embeddingModel.embed(
                "Database connection timeout"
        );

        float[] second = embeddingModel.embed(
                "Upstream pricing service unavailable"
        );

        assertEquals(first.length, second.length);
    }

    private double[] toDoubleArray(float[] values) {
        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }

        return result;
    }
}
