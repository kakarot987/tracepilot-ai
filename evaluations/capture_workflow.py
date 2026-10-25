"""Capture persisted workflow metadata into an evaluator-ready draft.

Usage:
  python evaluations/capture_workflow.py --workflow-id UUID --output evaluations/local-predictions.json

This script makes GET requests only. Cause labels must be annotated from model
outputs by a human; it never fills them from ground truth.
"""
import argparse
import json
import time
import urllib.request
from pathlib import Path

def get_json(base, path):
    with urllib.request.urlopen(base.rstrip("/") + path, timeout=15) as response:
        return json.load(response)

def capture(base, workflow_id):
    run = get_json(base, "/api/v1/workflows/" + workflow_id)
    incident_id = run["incidentId"]
    status = run["status"]
    if status in ("PENDING", "RUNNING"):
        raise ValueError("Workflow is still active; capture after completion")
    steps = get_json(base, "/api/v1/workflows/" + workflow_id + "/steps")
    evidence = set()
    for step in steps:
        step_id = step["id"]
        try:
            output = get_json(base, "/api/v1/workflows/" + workflow_id + "/steps/" + step_id + "/output")
        except Exception:
            continue
        investigation_id = output.get("investigationRunId")
        if investigation_id:
            try:
                refs = get_json(base, "/api/v1/investigations/" + investigation_id + "/evidence")
                evidence.update(ref["evidenceId"] for ref in refs if ref.get("evidenceId"))
            except Exception:
                pass
    started = run.get("startedAt")
    completed = run.get("completedAt")
    from datetime import datetime
    def timestamp(value):
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    latency = max(0, round((timestamp(completed) - timestamp(started)).total_seconds() * 1000)) if started and completed else None
    return {
        "incidentId": incident_id,
        "workflowId": workflow_id,
        "status": status,
        "latencyMs": latency,
        "predictedRootCause": None,
        "citedEvidenceIds": [],
        "retrievedEvidenceIds": sorted(evidence),
        "annotationRequired": True,
        "annotationNote": "Inspect final Reviewer outcome and hypotheses; enter model-predicted cause and only actually cited IDs. Do not copy gold labels.",
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workflow-id", required=True)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = capture(args.base_url, args.workflow_id)
    if args.output.exists():
        existing = json.loads(args.output.read_text(encoding="utf-8"))
        records = existing["predictions"] if isinstance(existing, dict) else existing
    else:
        records = []
    records = [entry for entry in records if entry["incidentId"] != result["incidentId"]]
    records.append(result)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"predictions": records}, indent=2) + "\n", encoding="utf-8")
    print(f"Captured {result['incidentId']} with {len(result['retrievedEvidenceIds'])} retrieved IDs; human annotation required.")

if __name__ == "__main__":
    main()
