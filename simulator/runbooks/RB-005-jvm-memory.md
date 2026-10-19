---
document_id: RB-005
title: JVM Heap Pressure Runbook
type: runbook
service: inventory-service
version: 1
updated_at: 2026-09-15
tags: [java, jvm, heap, out-of-memory, gc]
---

# JVM Heap Pressure Runbook

## Purpose
Investigate high JVM heap utilization, allocation failures, and memory-related restarts.

## Symptoms
- Heap utilization remains elevated
- Garbage collection frequency increases
- Allocation failures occur
- Java heap space errors appear
- Application instances restart unexpectedly

## Investigation
1. Inspect JVM logs immediately before the failure.
2. Determine whether memory use increased gradually or suddenly.
3. Compare heap usage with the configured maximum.
4. Review garbage collection and allocation metrics.
5. Examine recent deployments and workload changes.
6. Investigate batch sizes, retained objects, and large collections.

## Possible causes
- Memory leak
- Workload exceeding expected capacity
- Oversized batch processing
- Heap configuration mismatch
- Excessive in-memory caching

## Safe recommendations
Collect heap and allocation evidence before recommending an increased heap size. Consider bounded batch processing.

## Evidence limitations
High heap usage does not establish a memory leak without additional allocation and retention evidence.
