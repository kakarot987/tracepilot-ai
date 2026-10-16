
package com.tracepilot.llm;

import com.tracepilot.incident.dto.IncidentAnalysisRequest;
import com.tracepilot.incident.dto.IncidentAnalysisResponse;

public interface IncidentModelInvocation {
    IncidentAnalysisResponse invoke(
            IncidentAnalysisRequest request
    );
}
