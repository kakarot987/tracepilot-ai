
package com.tracepilot.incident.dto;

import java.util.List;

public record IncidentAnalysisResponse(
        String severity,
        String category,
        List<IncidentHypothesis> hypotheses,
        boolean requiresInvestigation
) {}
