
package com.tracepilot.rag.search;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Objects;

@Service
@Profile("postgres")
public class RunbookSearchService {

    private static final int DIMENSIONS = 768;

    private final EmbeddingModel embeddings;
    private final JdbcClient jdbc;

    @Value("${tracepilot.rag.min-similarity:0.0}")
    private double minSimilarity;
    @Value("${tracepilot.rag.embedding-model-id:embeddinggemma-local-v1}")
    private String embeddingModelId;

    public RunbookSearchService(
            EmbeddingModel embeddings,
            JdbcClient jdbc) {

        this.embeddings = Objects.requireNonNull(embeddings);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    public List<RunbookSearchResult> search(
            RunbookSearchRequest request) {

        Objects.requireNonNull(request);

        // Generate the search vector using the same
        // model used during document indexing.
        float[] vector = embeddings.embed(request.query());

        if (vector == null || vector.length != DIMENSIONS) {
            throw new IllegalStateException(
                    "Unexpected embedding dimension"
            );
        }

        String queryVector = vectorLiteral(vector);

        return jdbc.sql("""
                SELECT
                    c.chunk_id,
                    d.document_id,
                    d.title,
                    d.document_type,
                    d.service_name,
                    d.version,
                    d.source_path,
                    c.content,
                    1 - (
                        c.embedding <=> CAST(:embedding AS vector)
                    ) AS similarity
                FROM rag_chunks c
                JOIN rag_documents d
                  ON d.document_id = c.document_id
                WHERE
                    (
                        CAST(:service AS VARCHAR) IS NULL
                        OR d.service_name = :service
                    )
                  AND (
                        CAST(:documentType AS VARCHAR) IS NULL
                        OR d.document_type = :documentType
                    )
                  AND 1 - (
                         c.embedding <=> CAST(:embedding AS vector)
                    ) >= :minSimilarity
                  AND d.embedding_model = :embeddingModel
                ORDER BY
                    c.embedding <=> CAST(:embedding AS vector),
                    c.chunk_id
                LIMIT :limit
                """)
                .param("embedding", queryVector)
                .param("service", request.serviceName())
                .param("documentType", request.documentType())
                .param("limit", request.limit())
                .param("minSimilarity", minSimilarity)
                .param("embeddingModel", embeddingModelId)
                .query((rs, rowNum) ->
                        new RunbookSearchResult(
                                rs.getString("chunk_id"),
                                rs.getString("document_id"),
                                rs.getString("title"),
                                rs.getString("document_type"),
                                rs.getString("service_name"),
                                rs.getInt("version"),
                                rs.getString("source_path"),
                                rs.getString("content"),
                                rs.getDouble("similarity")
                        )
                )
                .list();
    }

    private String vectorLiteral(float[] values) {
        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < values.length; i++) {

            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException(
                        "Embedding contains non-finite values"
                );
            }

            if (i > 0) {
                result.append(',');
            }

            result.append(values[i]);
        }

        return result.append(']').toString();
    }
}
