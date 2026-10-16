
package com.tracepilot.llm;

import com.tracepilot.incident.dto.IncidentAnalysisRequest;
import com.tracepilot.incident.dto.IncidentAnalysisResponse;

public interface IncidentModelGateway {

    IncidentAnalysisResponse analyze(
            IncidentAnalysisRequest request
    );
}
