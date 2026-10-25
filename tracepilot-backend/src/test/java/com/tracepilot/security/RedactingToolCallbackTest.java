package com.tracepilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedactingToolCallbackTest {
    @Test
    void masksToolOutputWithoutChangingStoredEvidenceOrToolDefinition() {
        var source = mock(ToolCallback.class);
        var definition = mock(ToolDefinition.class);
        when(source.getToolDefinition()).thenReturn(definition);
        when(source.call("query")).thenReturn("LOG-PAY-002 password=hunter2");
        var wrapped = new RedactingToolCallback(source);

        assertSame(definition, wrapped.getToolDefinition());
        assertEquals("LOG-PAY-002 password=[REDACTED]", wrapped.call("query"));
        verify(source).call("query");
    }

    @Test
    void masksContextBasedToolOutput() {
        var source = mock(ToolCallback.class);
        var context = mock(ToolContext.class);
        when(source.call("query", context)).thenReturn("Bearer abc.def user@test.org");
        assertEquals("Bearer [REDACTED] [REDACTED_EMAIL]",
                new RedactingToolCallback(source).call("query", context));
    }
}
