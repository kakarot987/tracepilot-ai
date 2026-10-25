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

    @Test
    void masksAccessTokenAndRefreshToken() {
        String result = PromptRedactor.redact(
                "access_token=abc123 refresh_token:xyz890"
        );

        assertFalse(result.contains("abc123"));
        assertFalse(result.contains("xyz890"));

        assertTrue(
                result.contains("access_token=[REDACTED]")
        );

        assertTrue(
                result.contains("refresh_token:[REDACTED]")
        );
    }

    @Test
    void handlesNullInputWithoutThrowing() {
        assertNull(PromptRedactor.redact(null));
    }
}
