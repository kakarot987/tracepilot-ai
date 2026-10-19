---
document_id: PM-002
title: Historical Order Database Saturation
type: postmortem
service: order-service
version: 1
updated_at: 2026-09-20
tags: [orders, database, connection-pool]
---

# Historical Order Database Saturation

## Summary
A synthetic historical order-service incident produced intermittent JDBC connection timeouts during a batch processing window.

## Observed evidence
Pool utilization increased sharply while batch operations held transactions open. Query latency also increased.

## Historical finding
A newly scheduled batch task created additional simultaneous database activity and prolonged connection retention.

## Mitigation
The batch concurrency limit was reduced after workload analysis.

## Preventive actions
- Establish batch concurrency limits.
- Track active and pending connections.
- Alert on prolonged transaction durations.
- Include database capacity checks in batch rollout plans.

## Applicability
Similar errors can arise from other causes. Historical findings must not be treated as current incident evidence.
