
package com.tracepilot.rag.ingest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class RunbookParser {

    public ParsedRunbook parse(Path file) throws Exception {
        String raw = Files.readString(file, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        String[] parts = raw.split("(?m)^---\\s*$", 3);

        if (parts.length != 3 || !parts[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Missing YAML frontmatter: " + file
            );
        }

        Map<String, String> meta = new TreeMap<>();

        for (String line : parts[1].split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException(
                        "Invalid metadata in " + file
                );
            }
            meta.put(
                    line.substring(0, colon).trim(),
                    line.substring(colon + 1).trim()
            );
        }

        String documentId = require(meta, "document_id");
        String title = require(meta, "title");
        String type = require(meta, "type");
        String service = require(meta, "service");
        int version = Integer.parseInt(require(meta, "version"));

        if (!documentId.matches("(RB|PM)-\\d{3}")
                || !List.of("runbook", "postmortem").contains(type)
                || version < 1) {
            throw new IllegalArgumentException(
                    "Invalid runbook metadata: " + file
            );
        }

        String body = parts[2].trim();

        if (body.isEmpty()) {
            throw new IllegalArgumentException(
                    "Empty document: " + file
            );
        }

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hashed = digest.digest(
                raw.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder hex = new StringBuilder();
        for (byte b : hashed) {
            hex.append("%02x".formatted(b & 0xff));
        }

        return new ParsedRunbook(
                documentId,
                title,
                type,
                service,
                version,
                file.toString().replace('\\', '/'),
                hex.toString(),
                body
        );
    }

    private String require(
            Map<String, String> meta,
            String key) {

        String value = meta.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing metadata: " + key
            );
        }
        return value;
    }

    public List<String> chunk(String content) {
        final int maxChars = 850;

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph :
                content.split("\\n\\s*\\n")) {

            paragraph = paragraph.trim();

            if (paragraph.isBlank()) {
                continue;
            }

            // Handle paragraphs longer than the chunk limit.
            while (paragraph.length() > maxChars) {
                flush(current, chunks);
                chunks.add(paragraph.substring(0, maxChars));
                paragraph = paragraph.substring(maxChars);
            }

            if (current.length() > 0
                    && current.length() + paragraph.length() + 2
                    > maxChars) {
                flush(current, chunks);
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }

        flush(current, chunks);
        return List.copyOf(chunks);
    }

    private void flush(
            StringBuilder current,
            List<String> chunks) {

        if (current.length() > 0) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }

    public record ParsedRunbook(
            String documentId,
            String title,
            String type,
            String serviceName,
            int version,
            String sourcePath,
            String contentHash,
            String content
    ) {}
}
