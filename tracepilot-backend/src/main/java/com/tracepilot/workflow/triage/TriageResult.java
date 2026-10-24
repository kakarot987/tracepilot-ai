
package com.tracepilot.workflow.triage;

import java.util.List;

public record TriageResult(
        TriageSeverity severity,
        TriageCategory category,
        String investigationFocus,
        List<String> openQuestions
) {
    public TriageResult {
        if (severity == null || category == null) {
            throw new IllegalArgumentException(
                    "Severity and category are required");
        }

        if (investigationFocus == null
                || investigationFocus.isBlank()
                || investigationFocus.length() > 500) {
            throw new IllegalArgumentException(
                    "Invalid investigation focus");
        }

        if (openQuestions == null
                || openQuestions.isEmpty()
                || openQuestions.size() > 5) {
            throw new IllegalArgumentException(
                    "Expected 1 to 5 open questions");
        }

        for (String question : openQuestions) {
            if (question == null || question.isBlank()
                    || question.length() > 300) {
                throw new IllegalArgumentException(
                        "Invalid open question");
            }
        }

        openQuestions = List.copyOf(openQuestions);
    }
}
