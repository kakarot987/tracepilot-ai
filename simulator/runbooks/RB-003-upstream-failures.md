---
document_id: RB-003
title: Upstream Dependency Failure Runbook
type: runbook
service: checkout-service
version: 1
updated_at: 2026-09-15
tags: [checkout, upstream, http-502, http-503]
---

# Upstream Dependency Failure Runbook

## Purpose
Investigate failures in a service that relies on an external or internal HTTP dependency.

## Symptoms
- Increased HTTP 502 or 503 responses
- Slow upstream requests
- Connection or read timeouts
- Elevated retry and circuit breaker activity

## Investigation
1. Identify the first failed downstream request.
2. Compare downstream and upstream timestamps.
3. Check whether HTTP 503 originated from the dependency.
4. Inspect circuit breaker and retry behavior.
5. Check recent deployments of both services.
6. Review dependency health and incident history.

## Possible causes
- Upstream service outage
- Dependency resource exhaustion
- Network disruption
- Aggressive retry traffic
- Timeout configuration mismatch

## Safe recommendations
Confirm the failing dependency before considering retry changes. Use bounded retries and evaluate the risk of retry amplification.

## Evidence limitations
A downstream HTTP 502 response alone does not identify which dependency failed.
