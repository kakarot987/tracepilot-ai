
package com.tracepilot.rag.search;

public record RunbookSearchRequest(
        String query,
        Integer limit,
        String serviceName,
        String documentType
) {
    public RunbookSearchRequest {
        if (query == null || query.isBlank()
                || query.length() > 500) {
            throw new IllegalArgumentException(
                    "Query must contain 1 to 500 characters"
            );
        }

        query = query.trim();

        if (limit == null || limit < 1 || limit > 5) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 5"
            );
        }

        if (serviceName != null) {
            serviceName = serviceName.trim();

            if (!serviceName.matches("[a-z0-9-]{1,120}")) {
                throw new IllegalArgumentException(
                        "Invalid service filter"
                );
            }
        }

        if (documentType != null
                && !documentType.equals("runbook")
                && !documentType.equals("postmortem")) {
            throw new IllegalArgumentException(
                    "Invalid document type"
            );
        }
    }
}
