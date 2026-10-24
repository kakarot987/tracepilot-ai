
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.InvestigationBudget;
import com.tracepilot.investigation.domain.InvestigationLimitExceededException;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

@Component
public class BoundedModelRunner {

    private static final int MAX_MODEL_REQUESTS = 6;

    private final ChatModel model;
    private final ToolCallingManager toolCallingManager;
    private static final Logger log =
            LoggerFactory.getLogger(BoundedModelRunner.class);

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

        return run(
                systemInstruction,
                incidentPrompt,
                callbacks,
                budget,
                false
        );
    }

    public String run(
            String systemInstruction,
            String incidentPrompt,
            List<ToolCallback> callbacks,
            InvestigationBudget budget,
            boolean disableThinking) {

        return run(
                systemInstruction,
                incidentPrompt,
                callbacks,
                budget,
                disableThinking,
                false
        );
    }

    public String run(
            String systemInstruction,
            String incidentPrompt,
            List<ToolCallback> callbacks,
            InvestigationBudget budget,
            boolean disableThinking,
            boolean jsonMode) {

        Objects.requireNonNull(systemInstruction);
        Objects.requireNonNull(incidentPrompt);
        Objects.requireNonNull(callbacks);
        Objects.requireNonNull(budget);

        var optionsBuilder = OllamaChatOptions.builder()
                .model("qwen3:4b")
                .temperature(0.2)
                .numPredict(512)
                .toolCallbacks(callbacks);

        if (disableThinking) {
            optionsBuilder.disableThinking();
        }

        if (jsonMode) {
            optionsBuilder.format("json");
        }

        var options = optionsBuilder.build();

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
            long modelStart = System.nanoTime();

            try {
                var response = model.call(prompt);

                long modelDurationMs =
                        java.util.concurrent.TimeUnit.NANOSECONDS
                                .toMillis(System.nanoTime() - modelStart);

                log.info(
                        "Model call returned; turn={}, durationMs={}, "
                                + "toolCallsUsed={}, remainingToolCalls={}",
                        turn + 1,
                        modelDurationMs,
                        budget.toolCallsUsed(),
                        budget.remainingToolCalls()
                );

                checkDeadline(budget);

                // Keep the existing response validation and
                // tool-calling logic here.

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


                long toolStart = System.nanoTime();

                var toolResult =
                        toolCallingManager.executeToolCalls(
                                prompt, response
                        );

                long toolDurationMs =
                        java.util.concurrent.TimeUnit.NANOSECONDS
                                .toMillis(
                                        System.nanoTime() - toolStart
                                );

                log.info(
                        "Tool round completed; turn={}, durationMs={}, "
                                + "toolCallsUsed={}, remainingToolCalls={}",
                        turn + 1,
                        toolDurationMs,
                        budget.toolCallsUsed(),
                        budget.remainingToolCalls()
                );

                checkDeadline(budget);

                prompt = new Prompt(
                        toolResult.conversationHistory(),
                        options
                );

            } catch (RuntimeException ex) {
                log.warn(
                        "Model turn failed; turn={}, durationMs={}, "
                                + "exceptionType={}",
                        turn + 1,
                        java.util.concurrent.TimeUnit.NANOSECONDS
                                .toMillis(System.nanoTime() - modelStart),
                        ex.getClass().getSimpleName()
                );

                throw ex;
            }
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
