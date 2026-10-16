
package com.tracepilot.incident.api;

import com.tracepilot.common.api.ApiExceptionHandler;
import com.tracepilot.incident.dto.IncidentAnalysisRequest;
import com.tracepilot.incident.service.IncidentAnalysisService;
import com.tracepilot.llm.exception.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IncidentAnalysisControllerTest {

    private MockMvc mockMvc;
    private IncidentAnalysisService service;

    @BeforeEach
    void setup() {
        service = mock(IncidentAnalysisService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new IncidentAnalysisController(service)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static final String URL =
            "/api/v1/incidents/analyze";

    private static final String VALID_REQUEST = """
        {
          "title": "Payment timeout",
          "description": "HTTP 504 errors",
          "serviceName": "payment-service"
        }
        """;

    @Test
    void returns503WhenModelUnavailable() throws Exception {
        when(service.analyze(any(IncidentAnalysisRequest.class)))
                .thenThrow(new ModelUnavailableException(
                        "Connection refused", new RuntimeException()
                ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title")
                        .value("AI service unavailable"));
    }

    @Test
    void returns504WhenModelTimesOut() throws Exception {
        when(service.analyze(any(IncidentAnalysisRequest.class)))
                .thenThrow(new ModelTimeoutException(
                        "Read timeout", new RuntimeException()
                ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.title")
                        .value("AI request timed out"));
    }

    @Test
    void returns502WhenModelOutputInvalid() throws Exception {
        when(service.analyze(any(IncidentAnalysisRequest.class)))
                .thenThrow(new ModelOutputException(
                        "Invalid model output"
                ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title")
                        .value("Invalid AI response"));
    }

    @Test
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
