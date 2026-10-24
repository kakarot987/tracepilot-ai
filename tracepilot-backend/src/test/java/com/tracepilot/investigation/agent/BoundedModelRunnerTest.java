
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BoundedModelRunnerTest {

    private final ChatModel model = mock(ChatModel.class);
    private final ToolCallingManager manager =
            mock(ToolCallingManager.class);

    private BoundedModelRunner runner() {
        return new BoundedModelRunner(model, manager);
    }

    @Test
    void rejectsNullModelResponse() {
        when(model.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> runner().run(
                        "System instructions",
                        "Investigate INC-PAY-001",
                        List.of(),
                        new InvestigationBudget(
                                8, Duration.ofMinutes(1)
                        )
                )
        );

        verify(model, times(1)).call(
                any(org.springframework.ai.chat.prompt.Prompt.class)
        );
        verifyNoInteractions(manager);
    }

    @Test
    void stopsAfterSixModelRequests() {
        ChatResponse response = mock(ChatResponse.class);
        ToolExecutionResult toolResult =
                mock(ToolExecutionResult.class);

        when(response.hasToolCalls()).thenReturn(true);

        when(model.call(
                any(org.springframework.ai.chat.prompt.Prompt.class)
        )).thenReturn(response);

        when(manager.executeToolCalls(
                any(org.springframework.ai.chat.prompt.Prompt.class),
                eq(response)
        )).thenReturn(toolResult);

        when(toolResult.conversationHistory())
                .thenReturn(List.of(
                        new org.springframework.ai.chat.messages.UserMessage(
                                "Continue investigation"
                        )
                ));

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> runner().run(
                        "System instructions",
                        "Investigate INC-PAY-001",
                        List.of(),
                        new InvestigationBudget(
                                8, Duration.ofMinutes(1)
                        )
                )
        );

        verify(model, times(6)).call(
                any(org.springframework.ai.chat.prompt.Prompt.class)
        );

        // The sixth response is not allowed to start
        // another round of tool execution.
        verify(manager, times(5)).executeToolCalls(
                any(org.springframework.ai.chat.prompt.Prompt.class),
                eq(response)
        );
    }

    @Test
    void rejectsExpiredBudgetBeforeModelCall() {
        var budget = new InvestigationBudget(
                8, Duration.ofNanos(1)
        );

        assertThrows(
                InvestigationLimitExceededException.class,
                () -> runner().run(
                        "System instructions",
                        "Investigate INC-PAY-001",
                        List.of(),
                        budget
                )
        );

        verifyNoInteractions(model, manager);
    }

    @Test
    void rejectsModelResponseThatArrivesAfterDeadline() {

        var budget = new InvestigationBudget(
                1,
                Duration.ofMillis(30)
        );

        ChatResponse response = mock(ChatResponse.class);

        when(response.hasToolCalls()).thenReturn(false);

        when(model.call(
                any(org.springframework.ai.chat.prompt.Prompt.class)
        )).thenAnswer(invocation -> {

            // Simulate a synchronous model request that
            // completes only after the investigation deadline.
            Thread.sleep(100);

            return response;
        });

        var exception = assertThrows(
                InvestigationLimitExceededException.class,
                () -> runner().run(
                        "System instructions",
                        "Classify database incident",
                        List.of(),
                        budget
                )
        );

        assertEquals(
                "Investigation deadline exceeded",
                exception.getMessage()
        );

        verify(model, times(1)).call(
                any(org.springframework.ai.chat.prompt.Prompt.class)
        );

        // A late model result must not trigger tool execution.
        verifyNoInteractions(manager);
    }

}
