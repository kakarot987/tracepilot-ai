
package com.tracepilot.workflow.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("postgres")
public class WorkflowHeartbeatService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WorkflowHeartbeatService.class);

    private final WorkflowLeaseService leases;

    private final ConcurrentHashMap<UUID, Heartbeat>
            active = new ConcurrentHashMap<>();

    public WorkflowHeartbeatService(
            WorkflowLeaseService leases) {
        this.leases = Objects.requireNonNull(leases);
    }

    public Heartbeat start(WorkflowLease lease) {
        Objects.requireNonNull(lease);

        Heartbeat heartbeat = new Heartbeat(lease);

        Heartbeat existing = active.putIfAbsent(
                lease.workflowId(), heartbeat);

        if (existing != null) {
            throw new IllegalStateException(
                    "Workflow already registered for heartbeat");
        }

        return heartbeat;
    }

    @Scheduled(fixedDelay = 20000)
    public void renewActiveLeases() {

        for (Heartbeat heartbeat : active.values()) {

            if (heartbeat.closed.get()
                    || heartbeat.lost.get()) {
                continue;
            }

            try {
                boolean renewed = leases.renew(
                        heartbeat.lease);

                if (!renewed) {
                    heartbeat.lost.set(true);

                    log.warn(
                            "Workflow ownership lost: {}",
                            heartbeat.lease.workflowId()
                    );
                }

            } catch (RuntimeException ex) {
                // Fail closed; do not silently resume
                // this execution after a renewal error.
                heartbeat.lost.set(true);

                log.error(
                        "Workflow lease renewal failed: {}",
                        heartbeat.lease.workflowId(),
                        ex
                );
            }
        }
    }

    public final class Heartbeat
            implements AutoCloseable {

        private final WorkflowLease lease;
        private final AtomicBoolean lost =
                new AtomicBoolean(false);
        private final AtomicBoolean closed =
                new AtomicBoolean(false);

        private Heartbeat(WorkflowLease lease) {
            this.lease = lease;
        }

        public void assertHealthy() {
            if (closed.get() || lost.get()) {
                throw new IllegalStateException(
                        "Workflow heartbeat is no longer healthy");
            }
        }

        public boolean ownershipLost() {
            return lost.get();
        }

        @Override
        public void close() {
            closed.set(true);
            active.remove(lease.workflowId(), this);
        }
    }
}
