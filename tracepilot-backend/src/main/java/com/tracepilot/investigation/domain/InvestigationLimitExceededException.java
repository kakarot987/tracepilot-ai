
package com.tracepilot.investigation.domain;

public class InvestigationLimitExceededException
        extends RuntimeException {

    public InvestigationLimitExceededException(
            String message) {
        super(message);
    }
}
