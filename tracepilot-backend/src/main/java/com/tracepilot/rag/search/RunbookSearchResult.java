
package com.tracepilot.rag.search;

public record RunbookSearchResult(
        String chunkId,
        String documentId,
        String title,
        String documentType,
        String serviceName,
        int version,
        String sourcePath,
        String content,
        double similarity
) {}
