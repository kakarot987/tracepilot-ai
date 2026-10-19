
package com.tracepilot.rag.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_RAG_TEST",
        matches = "true"
)
class RunbookSearchIntegrationTest {

    @Autowired
    RunbookSearchService searchService;

    @Autowired
    JdbcClient jdbc;

    @Test
    void retrievesRelevantDatabaseRunbook() {

        long indexed = jdbc.sql("""
                SELECT COUNT(*) FROM rag_chunks
                """)
                .query(Long.class)
                .single();

        assertTrue(
                indexed > 0,
                "Runbooks must be indexed before this test"
        );

        List<RunbookSearchResult> results =
                searchService.search(
                        new RunbookSearchRequest(
                                "JDBC connection pool exhaustion "
                                        + "and database connection timeout",
                                5,
                                null,
                                "runbook"
                        )
                );

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 5);

        // The corpus contains a specifically relevant
        // database connection pool runbook.
        assertTrue(
                results.stream().anyMatch(
                        result -> "RB-002"
                                .equals(result.documentId())
                ),
                "Expected database runbook in top 5"
        );

        assertTrue(results.stream().allMatch(
                result -> "runbook".equals(
                        result.documentType()
                )
        ));
    }

    @Test
    void respectsServiceAndTypeFilters() {

        var results = searchService.search(
                new RunbookSearchRequest(
                        "payment service timeout",
                        3,
                        "payment-service",
                        "runbook"
                )
        );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                result ->
                        "payment-service".equals(
                                result.serviceName()
                        )
                                && "runbook".equals(
                                result.documentType()
                        )
        ));
    }

    @Test
    void returnsEmptyWhenFilterMatchesNothing() {

        var results = searchService.search(
                new RunbookSearchRequest(
                        "connection timeout",
                        3,
                        "nonexistent-service",
                        null
                )
        );

        assertTrue(results.isEmpty());
    }
}
