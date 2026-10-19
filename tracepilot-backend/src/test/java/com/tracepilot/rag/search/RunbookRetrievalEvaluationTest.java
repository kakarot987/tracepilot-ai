
package com.tracepilot.rag.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(
        named = "TRACEPILOT_RAG_TEST",
        matches = "true"
)
class RunbookRetrievalEvaluationTest {

    @Autowired
    RunbookSearchService search;

    @Test
    void measuresRetrievalQuality() throws Exception {

        Path file = Path.of(
                "../evaluations/milestone-3-retrieval.csv"
        );

        List<String> rows = Files.readAllLines(file);

        assertEquals(11, rows.size(),
                "Expected header plus ten evaluation cases");

        int recallAt3 = 0;
        int recallAt5 = 0;
        double reciprocalRankSum = 0;

        for (String row : rows.subList(1, rows.size())) {

            String[] fields = row.split(",", -1);
            assertEquals(3, fields.length);

            String caseId = fields[0];
            String query = fields[1];
            String expectedDocumentId = fields[2];

            var results = search.search(
                    new RunbookSearchRequest(
                            query, 5, null, null
                    )
            );

            // Evaluate the rank of the first relevant
            // chunk for this labelled document.
            int rank = -1;

            for (int i = 0; i < results.size(); i++) {
                if (expectedDocumentId.equals(
                        results.get(i).documentId())) {

                    rank = i + 1;
                    break;
                }
            }

            if (rank > 0 && rank <= 3) {
                recallAt3++;
            }

            if (rank > 0 && rank <= 5) {
                recallAt5++;
            }

            if (rank > 0) {
                reciprocalRankSum += 1.0 / rank;
            }

            System.out.printf(
                    "%s expected=%s rank=%d%n",
                    caseId, expectedDocumentId, rank
            );
        }

        int count = rows.size() - 1;

        double r3 = recallAt3 / (double) count;
        double r5 = recallAt5 / (double) count;
        double mrr = reciprocalRankSum / count;

        System.out.printf(
                "Recall@3=%.3f Recall@5=%.3f MRR@5=%.3f%n",
                r3, r5, mrr
        );

        // Baseline quality gates for this small corpus.
        // These are project targets, not measured results.
        assertTrue(r3 >= 0.70,
                "Recall@3 below baseline");
        assertTrue(r5 >= 0.90,
                "Recall@5 below baseline");
    }
}
