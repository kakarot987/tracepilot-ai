
package com.tracepilot.tools.common;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationToolExecutorTest {

    private final InvestigationToolExecutor executor =
            new InvestigationToolExecutor();

    private InvestigationBudget budget(int calls) {
        return new InvestigationBudget(
                calls, Duration.ofMinutes(1)
        );
    }

    @Test
    void returnsSuccessForEvidence() {
        var result = executor.execute(
                budget(2),
                () -> List.of("LOG-PAY-001")
        );

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertEquals(List.of("LOG-PAY-001"), result.data());
    }

    @Test
    void returnsNoDataForEmptySearch() {
        var result = executor.execute(
                budget(2),
                List::<String>of
        );

        assertEquals(ToolStatus.NO_DATA, result.status());
        assertTrue(result.data().isEmpty());
    }

    @Test
    void rejectsInvalidArgumentsWithoutCrashing() {
        var result = executor.<String>execute(
                budget(2),
                () -> {
                    throw new IllegalArgumentException("Invalid limit");
                }
        );

        assertEquals(
                ToolStatus.INVALID_ARGUMENTS,
                result.status()
        );
    }

    @Test
    void sanitizesUnexpectedToolErrors() {
        var result = executor.<String>execute(
                budget(2),
                () -> {
                    throw new IllegalStateException(
                            "Sensitive internal implementation detail"
                    );
                }
        );

        assertEquals(ToolStatus.ERROR, result.status());
        assertFalse(result.message().contains("Sensitive"));
    }

    @Test
    void stopsAfterMaximumCalls() {
        var budget = budget(2);

        executor.execute(budget, () -> List.of("A"));
        executor.execute(budget, () -> List.of("B"));

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> executor.execute(
                        budget, () -> List.of("C")
                )
        );

        assertEquals(2, budget.toolCallsUsed());
    }

    @Test
    void stopsWhenDeadlineExpires() {
        var budget = new InvestigationBudget(
                3, Duration.ofMillis(10)
        );

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> executor.execute(
                        budget,
                        () -> {
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(ex);
                            }
                            return List.of("A");
                        }
                )
        );
    }

    @Test
    void separateInvestigationsHaveSeparateBudgets() {
        var first = budget(1);
        var second = budget(1);

        executor.execute(first, () -> List.of("A"));
        executor.execute(second, () -> List.of("B"));

        assertEquals(0, first.remainingToolCalls());
        assertEquals(0, second.remainingToolCalls());
    }
}
