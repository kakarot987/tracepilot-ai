
package com.tracepilot.investigation.api;

import com.tracepilot.investigation.agent.AgentExecution;
import com.tracepilot.investigation.agent.InvestigationAgent;
import com.tracepilot.investigation.domain.EvidenceReference;
import com.tracepilot.investigation.domain.InvestigationRun;
import com.tracepilot.investigation.service.InvestigationService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1")
public class InvestigationController {

    private final InvestigationAgent agent;
    private final InvestigationService investigations;

    public InvestigationController(
            InvestigationAgent agent,
            InvestigationService investigations) {
        this.agent = agent;
        this.investigations = investigations;
    }

    @PostMapping("/incidents/{incidentId}/investigations")
    public ResponseEntity<AgentExecution> investigate(
            @PathVariable String incidentId) {

        AgentExecution result = agent.investigate(incidentId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/investigations/{runId}")
    public InvestigationRun get(
            @PathVariable UUID runId) {

        return investigations.get(runId);
    }

    @GetMapping("/investigations/{runId}/evidence")
    public List<EvidenceReference> evidence(
            @PathVariable UUID runId) {

        return investigations.evidence(runId);
    }
}
