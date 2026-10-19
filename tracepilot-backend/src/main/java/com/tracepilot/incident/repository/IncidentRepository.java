
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.Incident;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

    List<Incident> findAll();

    Optional<Incident> findById(String incidentId);
}
