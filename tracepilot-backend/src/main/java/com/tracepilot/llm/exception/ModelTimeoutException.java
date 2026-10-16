package com.tracepilot.llm.exception;

public class ModelTimeoutException
        extends RuntimeException {

    public ModelTimeoutException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
