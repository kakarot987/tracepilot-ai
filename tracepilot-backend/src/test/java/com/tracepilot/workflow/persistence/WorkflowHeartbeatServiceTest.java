
package com.tracepilot.workflow.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowHeartbeatServiceTest {

    private WorkflowLeaseService leases;
    private WorkflowHeartbeatService service;
    private WorkflowLease lease;

    @BeforeEach
    void setUp() {
        leases = mock(WorkflowLeaseService.class);

        service = new WorkflowHeartbeatService(
                leases
        );

        lease = new WorkflowLease(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                Instant.now().plusSeconds(60)
        );
    }

    @Test
    void renewsRegisteredWorkflow() {
        when(leases.renew(lease)).thenReturn(true);

        try (var heartbeat = service.start(lease)) {
            service.renewActiveLeases();

            assertFalse(heartbeat.ownershipLost());
            assertDoesNotThrow(
                    heartbeat::assertHealthy
            );
        }

        verify(leases, times(1)).renew(lease);
    }

    @Test
    void marksOwnershipLostWhenRenewalIsRejected() {
        when(leases.renew(lease)).thenReturn(false);

        try (var heartbeat = service.start(lease)) {
            service.renewActiveLeases();

            assertTrue(heartbeat.ownershipLost());

            assertThrows(
                    IllegalStateException.class,
                    heartbeat::assertHealthy
            );
        }
    }

    @Test
    void marksOwnershipLostOnDatabaseException() {
        when(leases.renew(lease))
                .thenThrow(
                        new IllegalStateException(
                                "Database unavailable")
                );

        try (var heartbeat = service.start(lease)) {
            service.renewActiveLeases();

            assertTrue(heartbeat.ownershipLost());
        }
    }

    @Test
    void doesNotRenewClosedHeartbeat() {
        var heartbeat = service.start(lease);

        heartbeat.close();

        service.renewActiveLeases();

        verify(leases, never()).renew(lease);
    }

    @Test
    void rejectsDuplicateHeartbeatRegistration() {
        try (var heartbeat = service.start(lease)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> service.start(lease)
            );
        }
    }

    @Test
    void allowsRegistrationAfterPreviousSessionCloses() {
        var first = service.start(lease);
        first.close();

        try (var second = service.start(lease)) {
            assertDoesNotThrow(
                    second::assertHealthy
            );
        }
    }

    @Test
    void stopsRenewingAfterOwnershipIsLost() {
        when(leases.renew(lease)).thenReturn(false);

        try (var heartbeat = service.start(lease)) {
            service.renewActiveLeases();
            service.renewActiveLeases();

            assertTrue(heartbeat.ownershipLost());
        }

        verify(leases, times(1)).renew(lease);
    }
}
