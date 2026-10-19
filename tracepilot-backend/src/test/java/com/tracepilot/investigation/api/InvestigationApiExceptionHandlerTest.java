
package com.tracepilot.investigation.api;

import com.tracepilot.investigation.service.InvestigationConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationApiExceptionHandlerTest {

    private final InvestigationApiExceptionHandler handler =
            new InvestigationApiExceptionHandler();

    @Test
    void mapsDuplicateRunToConflict() {
        var response = handler.conflict(
                new InvestigationConflictException(
                        "Already running"
                )
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(
                "INVESTIGATION_CONFLICT",
                response.getBody().get("error")
        );
    }

    @Test
    void doesNotExposeInternalExceptionDetails() {
        var response = handler.badRequest(
                new IllegalArgumentException(
                        "Internal debugging details"
                )
        );

        assertEquals(400, response.getStatusCode().value());
        assertFalse(
                response.getBody()
                        .toString()
                        .contains("Internal debugging details")
        );
    }
}
