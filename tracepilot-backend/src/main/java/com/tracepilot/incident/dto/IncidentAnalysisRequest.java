package com.tracepilot.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentAnalysisRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 4000)
        String description,

        @NotBlank
        @Size(max = 100)
        String serviceName
) {}
