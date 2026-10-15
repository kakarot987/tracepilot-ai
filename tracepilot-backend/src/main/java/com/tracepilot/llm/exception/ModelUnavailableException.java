package com.tracepilot.llm.exception;

public class ModelUnavailableException
        extends RuntimeException {

    public ModelUnavailableException(
            String message, Throwable cause) {
        super(message, cause);
    }
}