
package com.tracepilot.rag.ingest;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        String existingHash = jdbc.sql("""
                SELECT content_hash
                FROM rag_documents
                WHERE document_id = :id
                """)
                .param("id", document.documentId())
                .query(String.class)
                .optional()
                .orElse(null);

        // Repeat ingestion without creating new embeddings.
        if (document.contentHash().equals(existingHash)) {
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
                     content_hash)
                VALUES
                    (:id, :title, :type, :service,
                     :version, :path, :hash)
                ON CONFLICT (document_id)
                DO UPDATE SET
                    title = EXCLUDED.title,
                    document_type = EXCLUDED.document_type,
                    service_name = EXCLUDED.service_name,
                    version = EXCLUDED.version,
                    source_path = EXCLUDED.source_path,
                    content_hash = EXCLUDED.content_hash,
                    indexed_at = clock_timestamp()
                """)
                .param("id", document.documentId())
                .param("title", document.title())
                .param("type", document.type())
                .param("service", document.serviceName())
                .param("version", document.version())
                .param("path", path.getFileName().toString())
                .param("hash", document.contentHash())
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
}
