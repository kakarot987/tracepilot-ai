---
document_id: RB-001
title: Payment Request Timeout Runbook
type: runbook
service: payment-service
version: 1
updated_at: 2026-09-15
tags: [payment, timeout, jdbc, database]
---

# Payment Request Timeout Runbook

## Purpose
Investigate elevated latency, HTTP 504 responses, and stalled requests in the payment service.

## Symptoms
- Payment API requests exceed the configured deadline.
- Connection acquisition latency increases.
- JDBC pool waiting threads increase.
- Requests may fail before database queries execute.

## Investigation
1. Inspect payment-service ERROR and WARN logs for the incident window.
2. Identify the timestamp of the first delayed request.
3. Check whether database connections are unavailable or merely slow.
4. Compare connection pool active, idle, and pending metrics.
5. Examine recent deployments for pool or timeout configuration changes.
6. Check database health and dependency saturation.

## Possible causes
- Exhausted JDBC connection pool
- Slow or leaked database connections
- Database resource saturation
- Incorrect timeout configuration
- Slow upstream dependencies

## Safe recommendations
Gather pool metrics and connection acquisition timings. Review recent changes before recommending any configuration adjustment.

## Escalation
Contact the payments team if requests continue failing or payment state is uncertain.

## Evidence limitations
A timeout alone does not prove connection pool exhaustion. Correlate logs with pool and database metrics.
