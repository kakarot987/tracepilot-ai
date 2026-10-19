
package com.tracepilot.rag.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RunbookSearchRequestTest {

    @Test
    void acceptsValidRequest() {
        var request = new RunbookSearchRequest(
                "JDBC connection pool exhaustion",
                3,
                "payment-service",
                "runbook"
        );

        assertEquals(3, request.limit());
        assertEquals("payment-service", request.serviceName());
    }

    @Test
    void rejectsBlankQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RunbookSearchRequest(
                        "  ", 3, null, null
                )
        );
    }

    @Test
    void rejectsExcessiveLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RunbookSearchRequest(
                        "database timeout", 50, null, null
                )
        );
    }

    @Test
    void rejectsInvalidDocumentType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RunbookSearchRequest(
                        "database timeout", 3,
                        null, "secret"
                )
        );
    }

    @Test
    void rejectsInvalidServiceFilter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RunbookSearchRequest(
                        "database timeout", 3,
                        "payment-service' OR 1=1 --",
                        null
                )
        );
    }
}
