
package com.tracepilot.workflow.reviewer;

import java.util.List;
import java.util.Objects;

public record ReviewerResult(
        ReviewDecision decision,
        List<ClaimReview> claimReviews,
        List<String> unresolvedQuestions,
        String rationale
) {
    public ReviewerResult {
        Objects.requireNonNull(decision);
        Objects.requireNonNull(claimReviews);
        Objects.requireNonNull(unresolvedQuestions);

        claimReviews = List.copyOf(claimReviews);
        unresolvedQuestions = List.copyOf(unresolvedQuestions);

        if (claimReviews.isEmpty()
                || claimReviews.size() > 3) {
            throw new IllegalArgumentException(
                    "Expected 1 to 3 claim reviews");
        }

        if (unresolvedQuestions.size() > 5) {
            throw new IllegalArgumentException(
                    "Too many unresolved questions");
        }

        if (rationale == null || rationale.isBlank()
                || rationale.length() > 1000) {
            throw new IllegalArgumentException(
                    "Invalid reviewer rationale");
        }
    }
}
