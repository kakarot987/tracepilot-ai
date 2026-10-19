
package com.tracepilot.rag.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RunbookParserTest {

    @TempDir
    Path directory;

    private final RunbookParser parser = new RunbookParser();

    @Test
    void parsesMetadataAndContent() throws Exception {
        Path file = directory.resolve("RB-001.md");

        Files.writeString(file, """
                ---
                document_id: RB-001
                title: Database Runbook
                type: runbook
                service: payment-service
                version: 1
                ---
                # Database Runbook

                Check connection pool utilization.
                """);

        var result = parser.parse(file);

        assertEquals("RB-001", result.documentId());
        assertEquals("payment-service", result.serviceName());
        assertEquals(1, result.version());
        assertEquals(64, result.contentHash().length());
    }

    @Test
    void producesDeterministicBoundedChunks() {
        String text = "Connection pool timeout.\n\n"
                + "Investigate database connections.";

        var first = parser.chunk(text);
        var second = parser.chunk(text);

        assertEquals(first, second);
        assertFalse(first.isEmpty());

        assertTrue(first.stream()
                .allMatch(chunk -> chunk.length() <= 850));
    }

    @Test
    void rejectsMissingMetadata() throws Exception {
        Path file = directory.resolve("broken.md");

        Files.writeString(file, """
                # No frontmatter
                Missing metadata.
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(file)
        );
    }
}
