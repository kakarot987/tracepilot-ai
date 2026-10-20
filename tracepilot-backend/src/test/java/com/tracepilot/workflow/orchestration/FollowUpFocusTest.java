package com.tracepilot.workflow.orchestration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpFocusTest {

    @Test
    void acceptsValidQuestions() {
        var focus = new FollowUpFocus(
                List.of(
                        "Which transactions held connections?",
                        "Did pool usage rise after deployment?"
                )
        );

        assertEquals(2, focus.questions().size());

        assertTrue(focus.asPrompt().contains(
                "Which transactions held connections?"));
    }

    @Test
    void rejectsEmptyQuestions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowUpFocus(List.of())
        );
    }

    @Test
    void rejectsTooManyQuestions() {
        var questions = java.util.stream.IntStream
                .range(0, 6)
                .mapToObj(i -> "Question " + i)
                .toList();

        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowUpFocus(questions)
        );
    }

    @Test
    void rejectsOversizedQuestion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowUpFocus(
                        List.of("A".repeat(301))
                )
        );
    }

    @Test
    void removesDuplicateQuestions() {
        var focus = new FollowUpFocus(
                List.of(
                        "Inspect connections",
                        "Inspect connections"
                )
        );

        assertEquals(1, focus.questions().size());
    }

    @Test
    void defensivelyCopiesQuestions() {
        var source = new java.util.ArrayList<>(
                List.of("What changed?")
        );

        var focus = new FollowUpFocus(source);
        source.add("Another question");

        assertEquals(1, focus.questions().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> focus.questions().add("New question")
        );
    }
}