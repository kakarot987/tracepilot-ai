
package com.tracepilot.tools.common;

import java.util.List;
import java.util.Objects;

public record ToolResult<T>(
        ToolStatus status,
        List<T> data,
        String message
) {
    public ToolResult {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(data, "data is required");
        data = List.copyOf(data);
        message = message == null ? "" : message;
    }

    public static <T> ToolResult<T> success(List<T> data) {
        Objects.requireNonNull(data);

        if (data.isEmpty()) {
            return noData("No matching evidence found");
        }

        return new ToolResult<>(
                ToolStatus.SUCCESS, data, ""
        );
    }

    public static <T> ToolResult<T> noData(String message) {
        return new ToolResult<>(
                ToolStatus.NO_DATA, List.of(), message
        );
    }

    public static <T> ToolResult<T> invalid(String message) {
        return new ToolResult<>(
                ToolStatus.INVALID_ARGUMENTS, List.of(), message
        );
    }

    public static <T> ToolResult<T> error(String message) {
        return new ToolResult<>(
                ToolStatus.ERROR, List.of(), message
        );
    }
}
