
INSERT INTO log_evidence
(evidence_id, incident_id, service_name, occurred_at, level, message)
VALUES
('LOG-PAY-001', 'INC-PAY-001', 'payment-service',
 '2026-10-01T10:00:00Z', 'INFO', 'Payment request received'),
('LOG-PAY-002', 'INC-PAY-001', 'payment-service',
 '2026-10-01T10:00:03Z', 'WARN', 'Waiting for database connection'),
('LOG-PAY-003', 'INC-PAY-001', 'payment-service',
 '2026-10-01T10:00:08Z', 'ERROR',
 'Connection pool acquisition timeout'),

('LOG-DB-001', 'INC-DB-001', 'order-service',
 '2026-10-02T11:00:00Z', 'INFO', 'Order request received'),
('LOG-DB-002', 'INC-DB-001', 'order-service',
 '2026-10-02T11:00:03Z', 'WARN',
 'Database connection acquisition delayed'),
('LOG-DB-003', 'INC-DB-001', 'order-service',
 '2026-10-02T11:00:08Z', 'ERROR',
 'Unable to acquire JDBC connection within 3000ms'),

('LOG-UP-001', 'INC-UP-001', 'checkout-service',
 '2026-10-03T12:00:00Z', 'INFO', 'Checkout request received'),
('LOG-UP-002', 'INC-UP-001', 'checkout-service',
 '2026-10-03T12:00:02Z', 'WARN',
 'External pricing service response delayed'),
('LOG-UP-003', 'INC-UP-001', 'checkout-service',
 '2026-10-03T12:00:05Z', 'ERROR',
 'Upstream pricing request failed with HTTP 503'),

('LOG-RATE-001', 'INC-RATE-001', 'api-gateway',
 '2026-10-04T13:00:00Z', 'INFO', 'Incoming request rate increased'),
('LOG-RATE-002', 'INC-RATE-001', 'api-gateway',
 '2026-10-04T13:00:02Z', 'WARN',
 'Rate limit threshold exceeded for client group'),
('LOG-RATE-003', 'INC-RATE-001', 'api-gateway',
 '2026-10-04T13:00:04Z', 'ERROR',
 'Request rejected with HTTP 429'),

('LOG-MEM-001', 'INC-MEM-001', 'inventory-service',
 '2026-10-05T14:00:00Z', 'INFO',
 'Inventory synchronization started'),
('LOG-MEM-002', 'INC-MEM-001', 'inventory-service',
 '2026-10-05T14:00:03Z', 'WARN',
 'JVM heap usage above 90 percent'),
('LOG-MEM-003', 'INC-MEM-001', 'inventory-service',
 '2026-10-05T14:00:07Z', 'ERROR',
 'Java heap space allocation failed');
