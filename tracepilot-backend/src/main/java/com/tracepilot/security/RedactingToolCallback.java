package com.tracepilot.security;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/** Redacts the tool result sent to the model, leaving tool execution and storage untouched. */
public final class RedactingToolCallback implements ToolCallback {
    private final ToolCallback delegate;

    public RedactingToolCallback(ToolCallback delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String input) {
        return PromptRedactor.redact(delegate.call(input));
    }

    @Override
    public String call(String input, ToolContext context) {
        return PromptRedactor.redact(delegate.call(input, context));
    }
}
