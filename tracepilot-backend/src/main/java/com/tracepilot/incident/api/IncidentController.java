
package com.tracepilot.incident.api;

import com.tracepilot.incident.domain.Incident;
import com.tracepilot.incident.repository.IncidentRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentRepository incidents;

    public IncidentController(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    @GetMapping
    public List<Incident> list() {
        return incidents.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> get(@PathVariable String id) {
        return ResponseEntity.of(incidents.findById(id));
    }
}
