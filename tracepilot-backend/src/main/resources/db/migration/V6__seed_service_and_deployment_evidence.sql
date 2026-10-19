
INSERT INTO service_info (
    service_name, team, environment, runtime,
    database_dependency, upstream_dependency
)
VALUES
('payment-service', 'Payments', 'simulation',
 'Java 21 / Spring Boot', 'payments-db', 'payment-provider'),
('order-service', 'Orders', 'simulation',
 'Java 21 / Spring Boot', 'orders-db', NULL),
('checkout-service', 'Checkout', 'simulation',
 'Java 21 / Spring Boot', 'checkout-db', 'pricing-service'),
('api-gateway', 'Platform', 'simulation',
 'Spring Cloud Gateway', NULL, 'internal-services'),
('inventory-service', 'Inventory', 'simulation',
 'Java 21 / Spring Boot', 'inventory-db', NULL);

INSERT INTO deployment_evidence (
    evidence_id, service_name, version, deployed_at,
    status, change_summary
)
VALUES
('DEP-PAY-001', 'payment-service', '1.8.2',
 '2026-10-01T09:30:00Z', 'SUCCESS',
 'Updated payment request timeout configuration'),
('DEP-DB-001', 'order-service', '2.4.0',
 '2026-10-02T10:20:00Z', 'SUCCESS',
 'Released order validation changes'),
('DEP-UP-001', 'checkout-service', '3.1.5',
 '2026-10-03T09:00:00Z', 'SUCCESS',
 'Updated checkout telemetry'),
('DEP-RATE-001', 'api-gateway', '4.2.1',
 '2026-10-04T12:40:00Z', 'SUCCESS',
 'Adjusted traffic throttling configuration'),
('DEP-MEM-001', 'inventory-service', '1.6.0',
 '2026-10-05T12:00:00Z', 'SUCCESS',
 'Updated inventory synchronization scheduler');
