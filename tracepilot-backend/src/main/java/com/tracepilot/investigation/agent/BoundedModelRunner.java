
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BoundedModelRunner {

    private static final int MAX_MODEL_REQUESTS = 6;

    private final ChatModel model;
    private final ToolCallingManager toolCallingManager;

    public BoundedModelRunner(
            ChatModel model,
            ToolCallingManager toolCallingManager) {
        this.model = Objects.requireNonNull(model);
        this.toolCallingManager =
                Objects.requireNonNull(toolCallingManager);
    }

    public String run(
            String systemInstruction,
            String incidentPrompt,
            List<ToolCallback> callbacks,
            InvestigationBudget budget) {

        Objects.requireNonNull(systemInstruction);
        Objects.requireNonNull(incidentPrompt);
        Objects.requireNonNull(callbacks);
        Objects.requireNonNull(budget);

        var options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .build();

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(incidentPrompt)
                ),
                options
        );

        for (int turn = 0; turn < MAX_MODEL_REQUESTS; turn++) {

            checkDeadline(budget);

            /*
             * ChatModel does not automatically execute tools
             * in Spring AI 2.x.
             */
            var response = model.call(prompt);

            checkDeadline(budget);

            if (response == null) {
                throw new IllegalStateException(
                        "Model returned no response"
                );
            }

            if (!response.hasToolCalls()) {
                var output = response.getResult();

                if (output == null
                        || output.getOutput() == null) {
                    return "";
                }

                String content =
                        output.getOutput().getText();

                return content == null ? "" : content;
            }

            if (turn == MAX_MODEL_REQUESTS - 1) {
                throw new InvestigationLimitExceededException(
                        "Maximum model requests exceeded"
                );
            }

            checkDeadline(budget);

            var toolResult =
                    toolCallingManager.executeToolCalls(
                            prompt, response
                    );

            checkDeadline(budget);

            prompt = new Prompt(
                    toolResult.conversationHistory(),
                    options
            );
        }

        throw new InvestigationLimitExceededException(
                "Maximum model requests exceeded"
        );
    }

    private void checkDeadline(
            InvestigationBudget budget) {

        if (budget.isExpired()) {
            throw new InvestigationLimitExceededException(
                    "Investigation deadline exceeded"
            );
        }
    }
}
