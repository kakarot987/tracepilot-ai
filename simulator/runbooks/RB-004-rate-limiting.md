---
document_id: RB-004
title: API Rate Limiting Runbook
type: runbook
service: api-gateway
version: 1
updated_at: 2026-09-15
tags: [api-gateway, rate-limit, http-429, traffic]
---

# API Rate Limiting Runbook

## Purpose
Investigate HTTP 429 responses and unexpected client throttling.

## Symptoms
- Increasing HTTP 429 response counts
- Request rates approaching configured thresholds
- Client-specific throttling
- Bursts of rejected requests

## Investigation
1. Review incoming request volume by client group.
2. Inspect rate-limit decision logs.
3. Check the effective limit, time window, and client identity.
4. Compare traffic patterns with historical baselines.
5. Review recent gateway configuration deployments.
6. Identify whether the behavior is expected protection or misconfiguration.

## Possible causes
- Legitimate traffic bursts
- Client retry loops
- Abusive traffic
- Incorrect rate-limit configuration
- Shared client identity causing unintended throttling

## Safe recommendations
Prefer client backoff and request shaping when the configured threshold is intentional.

## Avoid
Do not disable rate limiting merely because HTTP 429 responses increased.

## Evidence limitations
An HTTP 429 response can reflect correctly functioning protection rather than a service outage.
