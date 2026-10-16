
INSERT INTO incidents (
    id, title, description, service_name, occurred_at
)
VALUES
(
    'INC-PAY-001',
    'Payment request timeouts',
    'Payment requests intermittently return HTTP 504.',
    'payment-service',
    '2026-10-01T10:00:00Z'
),
(
    'INC-DB-001',
    'Order database connection failures',
    'Order creation requests fail during peak traffic.',
    'order-service',
    '2026-10-02T11:00:00Z'
),
(
    'INC-UP-001',
    'Checkout upstream failures',
    'Checkout API returns intermittent HTTP 502 responses.',
    'checkout-service',
    '2026-10-03T12:00:00Z'
),
(
    'INC-RATE-001',
    'API gateway throttling',
    'Clients report unexpected HTTP 429 responses.',
    'api-gateway',
    '2026-10-04T13:00:00Z'
),
(
    'INC-MEM-001',
    'Inventory memory pressure',
    'Inventory service latency increases before restarts.',
    'inventory-service',
    '2026-10-05T14:00:00Z'
);
