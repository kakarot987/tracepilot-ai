
package com.tracepilot.rag.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@Profile("postgres")
@ConditionalOnProperty(
        name = "tracepilot.rag.index-enabled",
        havingValue = "true"
)
public class RunbookIndexCommand
        implements ApplicationRunner {
    @Value("${tracepilot.rag.directory:../simulator/runbooks}")
    private String directory;

    private final RunbookIndexer indexer;

    public RunbookIndexCommand(RunbookIndexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public void run(ApplicationArguments args)
            throws Exception {

        Path directory = Path.of(this.directory);

        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException(
                    "Runbook directory not found: " + directory
            );
        }

        try (Stream<Path> files = Files.list(directory)) {
            var documents = files
                    .filter(p -> p.getFileName()
                            .toString().endsWith(".md"))
                    .sorted()
                    .toList();

            if (documents.isEmpty()) {
                throw new IllegalStateException(
                        "No Markdown documents found"
                );
            }

            for (Path document : documents) {
                var result = indexer.index(document);

                System.out.printf(
                        "Indexed %s: chunks=%d, unchanged=%s%n",
                        result.documentId(),
                        result.chunksIndexed(),
                        result.unchanged()
                );
            }
        }
    }
}
