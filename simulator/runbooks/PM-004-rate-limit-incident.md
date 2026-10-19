---
document_id: PM-004
title: Historical Gateway Throttling Incident
type: postmortem
service: api-gateway
version: 1
updated_at: 2026-09-20
tags: [gateway, rate-limit, http-429]
---

# Historical Gateway Throttling Incident

## Summary
A synthetic historical gateway incident produced unexpected HTTP 429 responses for a group of otherwise healthy clients.

## Observed evidence
Logs showed multiple clients sharing the same rate-limit identity after a gateway configuration update.

## Historical finding
A configuration error grouped distinct clients under a shared throttling key.

## Mitigation
The team corrected client identification and tested the effective throttling rules.

## Preventive actions
- Test limit keys during gateway releases.
- Observe client-level rejection metrics.
- Compare rejected traffic against incoming traffic.

## Applicability
HTTP 429 responses may be expected during a genuine traffic spike. The historical configuration defect is not a universal explanation.
