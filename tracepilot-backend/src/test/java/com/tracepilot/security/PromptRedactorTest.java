package com.tracepilot.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromptRedactorTest {
    @Test
    void removesSensitiveValuesWithoutRemovingEvidenceIds() {
        String raw = "LOG-DB-003 password=supersecret api_key:abc123 " +
                "Bearer abc.def.ghi user=alice@example.org";
        String safe = PromptRedactor.redact(raw);
        assertTrue(safe.contains("LOG-DB-003"));
        assertTrue(safe.contains("password=[REDACTED]"));
        assertTrue(safe.contains("api_key:[REDACTED]"));
        assertTrue(safe.contains("Bearer [REDACTED]"));
        assertTrue(safe.contains("[REDACTED_EMAIL]"));
        assertFalse(safe.contains("supersecret"));
        assertFalse(safe.contains("abc.def.ghi"));
        assertFalse(safe.contains("alice@example.org"));
    }

    @Test
    void preservesOrdinaryLogDescriptions() {
        assertEquals("JDBC connection timed out", PromptRedactor.redact("JDBC connection timed out"));
    }
}
