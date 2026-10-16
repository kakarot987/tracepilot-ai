
package com.tracepilot.tools.history;

public record GetIncidentHistoryRequest(
        String incidentId,
        Integer limit
) {}
