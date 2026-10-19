INSERT INTO incident_history
    (evidence_id, incident_id, service_name,
     occurred_at, title, summary)
VALUES
('HIST-PAY-001', 'INC-HIST-PAY-001', 'payment-service',
 '2026-09-20T09:10:00Z', 'Intermittent payment latency',
 'Payment requests experienced elevated latency during a short traffic spike.'),

('HIST-PAY-002', 'INC-HIST-PAY-002', 'payment-service',
 '2026-09-27T11:30:00Z', 'Payment timeout alerts',
 'Several payment requests returned HTTP 504.'),

('HIST-DB-001', 'INC-HIST-DB-001', 'order-service',
 '2026-09-18T08:00:00Z', 'Order creation errors',
 'Order requests intermittently failed while waiting for a database connection.'),

('HIST-UP-001', 'INC-HIST-UP-001', 'checkout-service',
 '2026-09-15T14:30:00Z', 'Checkout dependency latency',
 'Checkout requests showed elevated upstream dependency response times.'),

('HIST-RATE-001', 'INC-HIST-RATE-001', 'api-gateway',
 '2026-09-19T13:00:00Z', 'Gateway response anomalies',
 'Clients reported increased HTTP 429 responses.'),

('HIST-MEM-001', 'INC-HIST-MEM-001', 'inventory-service',
 '2026-09-21T15:00:00Z', 'Inventory restarts',
 'Inventory requests slowed before service restarts.');