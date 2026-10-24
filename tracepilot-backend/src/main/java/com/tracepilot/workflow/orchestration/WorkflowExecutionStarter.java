
package com.tracepilot.workflow.orchestration;

import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowLeaseService;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
public class WorkflowExecutionStarter {

    private final WorkflowPersistenceService workflows;
    private final WorkflowLeaseService leases;

    public WorkflowExecutionStarter(
            WorkflowPersistenceService workflows,
            WorkflowLeaseService leases) {

        this.workflows = Objects.requireNonNull(workflows);
        this.leases = Objects.requireNonNull(leases);
    }

    @Transactional
    public WorkflowLease start(String incidentId) {

        var workflow = workflows.start(incidentId);

        return leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Initial workflow lease acquisition failed"
                )
        );
    }
}
