
package com.tracepilot.incident.service;

import com.tracepilot.incident.dto.*;
import com.tracepilot.llm.IncidentModelGateway;
import com.tracepilot.llm.exception.ModelOutputException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentAnalysisServiceTest {

    private final IncidentModelGateway gateway =
            mock(IncidentModelGateway.class);

    private final IncidentAnalysisService service =
            new IncidentAnalysisService(gateway);

    private final IncidentAnalysisRequest request =
            new IncidentAnalysisRequest(
                    "Payment timeout",
                    "Payments return HTTP 504",
                    "payment-service"
            );

    @Test
    void acceptsValidAnalysis() {
        var expected = new IncidentAnalysisResponse(
                "HIGH",
                "DATABASE",
                List.of(new IncidentHypothesis(
                        "Database connection exhaustion",
                        "Connections may be saturated"
                )),
                true
        );

        when(gateway.analyze(request)).thenReturn(expected);

        assertEquals(expected, service.analyze(request));
        verify(gateway).analyze(request);
    }

    @Test
    void rejectsInvalidAnalysis() {
        when(gateway.analyze(request)).thenReturn(
                new IncidentAnalysisResponse(
                        "INVALID", "DATABASE", List.of(), true
                )
        );

        assertThrows(
                ModelOutputException.class,
                () -> service.analyze(request)
        );
    }


    @Test
    void rejectsEmptyModelResponse() {
        when(gateway.analyze(request)).thenReturn(null);

        assertThrows(
                ModelOutputException.class,
                () -> service.analyze(request)
        );
    }

    @Test
    void rejectsInvalidSeverity() {
        var response = new IncidentAnalysisResponse(
                "EXTREME",
                "DATABASE",
                List.of(new IncidentHypothesis(
                        "Connection pool exhaustion",
                        "All connections are occupied"
                )),
                true
        );

        when(gateway.analyze(request)).thenReturn(response);

        assertThrows(
                ModelOutputException.class,
                () -> service.analyze(request)
        );
    }

    @Test
    void rejectsEmptyHypotheses() {
        when(gateway.analyze(request)).thenReturn(
                new IncidentAnalysisResponse(
                        "HIGH", "DATABASE", List.of(), true
                )
        );

        assertThrows(
                ModelOutputException.class,
                () -> service.analyze(request)
        );
    }

}
