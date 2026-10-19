
package com.tracepilot.rag.ingest;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service
@Profile("postgres")
public class RunbookIndexer {

    private static final int DIMENSIONS = 768;

    private final RunbookParser parser;
    private final EmbeddingModel embeddings;
    private final JdbcClient jdbc;

    @Value("${tracepilot.rag.embedding-model-id:embeddinggemma-local-v1}")
    private String embeddingModelId;

    public RunbookIndexer(
            RunbookParser parser,
            EmbeddingModel embeddings,
            JdbcClient jdbc) {

        this.parser = Objects.requireNonNull(parser);
        this.embeddings = Objects.requireNonNull(embeddings);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional(rollbackFor = Exception.class)
    public IndexResult index(Path path) throws Exception {

        var document = parser.parse(path);

        var existing = jdbc.sql("""
        SELECT content_hash, embedding_model
        FROM rag_documents
        WHERE document_id = :id
        """)
                .param("id", document.documentId())
                .query((rs, rowNum) -> new ExistingIndex(
                        rs.getString("content_hash"),
                        rs.getString("embedding_model")
                ))
                .optional();

        boolean unchanged = existing
                .map(index ->
                        document.contentHash().equals(index.contentHash())
                                && embeddingModelId.equals(index.embeddingModel()))
                .orElse(false);

        if (unchanged) {
            return new IndexResult(
                    document.documentId(), 0, true
            );
        }

        List<String> chunks =
                parser.chunk(document.content());

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No indexable content"
            );
        }

        // Generate all embeddings before modifying rows.
        var vectors = new java.util.ArrayList<String>();

        for (String chunk : chunks) {
            float[] vector = embeddings.embed(
                    document.title() + "\n" + chunk
            );

            if (vector == null ||
                    vector.length != DIMENSIONS) {
                throw new IllegalStateException(
                        "Unexpected embedding dimension"
                );
            }

            vectors.add(vectorLiteral(vector));
        }

        jdbc.sql("""
                        INSERT INTO rag_documents
                            (document_id, title, document_type,
                             service_name, version, source_path,
                             content_hash, embedding_model)
                        VALUES
                            (:id, :title, :type,
                             :service, :version, :path,
                             :hash, :embeddingModel)
                        ON CONFLICT (document_id)
                        DO UPDATE SET
                            title = EXCLUDED.title,
                            document_type = EXCLUDED.document_type,
                            service_name = EXCLUDED.service_name,
                            version = EXCLUDED.version,
                            source_path = EXCLUDED.source_path,
                            content_hash = EXCLUDED.content_hash,
                            embedding_model = EXCLUDED.embedding_model,
                            indexed_at = clock_timestamp()
                """)
                .param("id", document.documentId())
                .param("title", document.title())
                .param("type", document.type())
                .param("service", document.serviceName())
                .param("version", document.version())
                .param("path", path.getFileName().toString())
                .param("hash", document.contentHash())
                .param("embeddingModel", embeddingModelId)
                .update();

        jdbc.sql("""
                DELETE FROM rag_chunks
                WHERE document_id = :id
                """)
                .param("id", document.documentId())
                .update();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = "%s-v%d-c%03d".formatted(
                    document.documentId(),
                    document.version(),
                    i
            );

            jdbc.sql("""
                    INSERT INTO rag_chunks
                        (chunk_id, document_id,
                         chunk_index, content, embedding)
                    VALUES
                        (:chunkId, :documentId,
                         :chunkIndex, :content,
                         CAST(:embedding AS vector))
                    """)
                    .param("chunkId", chunkId)
                    .param("documentId", document.documentId())
                    .param("chunkIndex", i)
                    .param("content", chunks.get(i))
                    .param("embedding", vectors.get(i))
                    .update();
        }

        return new IndexResult(
                document.documentId(),
                chunks.size(),
                false
        );
    }

    private String vectorLiteral(float[] vector) {
        StringBuilder text = new StringBuilder("[");

        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) {
                throw new IllegalArgumentException(
                        "Non-finite embedding value"
                );
            }

            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }

        return text.append(']').toString();
    }

    public record IndexResult(
            String documentId,
            int chunksIndexed,
            boolean unchanged
    ) {}

    private record ExistingIndex(
            String contentHash,
            String embeddingModel
    ) {}
}
