package com.tracepilot.llm.exception;

public class ModelOutputException
        extends RuntimeException {

    public ModelOutputException(String message) {
        super(message);
    }

    public ModelOutputException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
