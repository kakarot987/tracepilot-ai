
package com.tracepilot.incident.api;

import com.tracepilot.incident.dto.*;
import com.tracepilot.incident.service.IncidentAnalysisService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentAnalysisController {

    private final IncidentAnalysisService service;

    public IncidentAnalysisController(
            IncidentAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public ResponseEntity<IncidentAnalysisResponse> analyze(
            @Valid @RequestBody
            IncidentAnalysisRequest request) {

        return ResponseEntity.ok(
                service.analyze(request)
        );
    }
}
