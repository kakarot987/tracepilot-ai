
package com.tracepilot.common.api;

import com.tracepilot.llm.exception.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler =
            new ApiExceptionHandler();

    @Test
    void mapsUnavailableTo503() {
        var response = handler.unavailable(
                new ModelUnavailableException(
                        "Connection refused",
                        new RuntimeException()
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );
    }

    @Test
    void mapsTimeoutTo504() {
        var response = handler.timeout(
                new ModelTimeoutException(
                        "Read timeout",
                        new RuntimeException()
                )
        );

        assertEquals(
                HttpStatus.GATEWAY_TIMEOUT,
                response.getStatusCode()
        );
    }

    @Test
    void mapsMalformedOutputTo502() {
        var response = handler.invalidOutput(
                new ModelOutputException("Invalid JSON")
        );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                response.getStatusCode()
        );
    }
}
