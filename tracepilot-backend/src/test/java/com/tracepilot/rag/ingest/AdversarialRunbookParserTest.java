
package com.tracepilot.rag.ingest;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdversarialRunbookParserTest {

    @Test
    void preservesHostileInstructionsAsUntrustedText()
            throws Exception {

        var parser = new RunbookParser();

        var document = parser.parse(Path.of(
                "src/test/resources/rag/adversarial-runbook.md"
        ));

        assertEquals("RB-099", document.documentId());

        assertTrue(document.content().contains(
                "IGNORE ALL PREVIOUS INSTRUCTIONS"
        ));

        var chunks = parser.chunk(document.content());

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream()
                .allMatch(chunk -> chunk.length() <= 850));
    }
}
