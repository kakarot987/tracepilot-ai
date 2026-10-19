---
document_id: RB-002
title: JDBC Connection Pool Exhaustion Runbook
type: runbook
service: order-service
version: 1
updated_at: 2026-09-15
tags: [jdbc, database, hikari, connection-pool]
---

# JDBC Connection Pool Exhaustion Runbook

## Purpose
Investigate database connection acquisition failures in Java services.

## Symptoms
- Messages indicating a connection was not available within the configured wait time
- Increasing request latency
- Pending connection requests
- Reduced throughput under load

## Investigation
1. Retrieve service logs for the affected period.
2. Compare active connections to the configured pool maximum.
3. Check pending requests and connection acquisition latency.
4. Examine transaction durations and slow queries.
5. Review deployment history for configuration changes.
6. Check database maximum connections and resource utilization.

## Diagnostic interpretation
Pool exhaustion may be caused by insufficient capacity, long transactions, leaked connections, or downstream database slowness.

## Safe recommendations
Measure utilization first. Investigate long-lived transactions and confirm the database can handle any proposed pool-size increase.

## Avoid
Do not recommend increasing the pool size solely from a single connection timeout log.

## Escalation
Escalate to database operations if the database reports connection saturation or sustained slow queries.
