"""Offline evaluator for captured TracePilot workflow predictions.

Input: JSON array or {"predictions": [...]} with one row per observed run.
Required: incidentId, workflowId, status, latencyMs, predictedRootCause,
citedEvidenceIds. Optional: toolCalls, successfulToolCalls.
Does not run Ollama, infer causes from free text, or conflate valid IDs with
semantic entailment.
"""
import argparse
import json
import statistics
from pathlib import Path

DEFAULT_GOLD = Path(__file__).with_name("milestone-6-incident-ground-truth.json")
TERMINAL = {"COMPLETED", "INSUFFICIENT_EVIDENCE", "FAILED", "TIMED_OUT", "CANCELLED"}


def evaluate(predictions, gold):
    by_id = {case["incidentId"]: case for case in gold}
    seen = set()
    rows = []
    for prediction in predictions:
        incident_id = prediction["incidentId"]
        if incident_id not in by_id:
            raise ValueError(f"Unknown incident: {incident_id}")
        if incident_id in seen:
            raise ValueError(f"Duplicate incident prediction: {incident_id}")
        seen.add(incident_id)
        case = by_id[incident_id]
        status = prediction["status"]
        if status not in TERMINAL:
            raise ValueError(f"Nonterminal workflow {incident_id}: {status}")
        latency = prediction["latencyMs"]
        if not isinstance(latency, (int, float)) or isinstance(latency, bool) or latency < 0:
            raise ValueError(f"Invalid latency for {incident_id}")
        citations = prediction["citedEvidenceIds"]
        if not isinstance(citations, list) or any(not isinstance(c, str) for c in citations):
            raise ValueError(f"Invalid citations for {incident_id}")
        if len(citations) != len(set(citations)):
            raise ValueError(f"Duplicate citations for {incident_id}")
        predicted = prediction["predictedRootCause"]
        if predicted is not None and not isinstance(predicted, str):
            raise ValueError(f"Invalid prediction for {incident_id}")
        expected = case["rootCauseLabel"]
        abstained = status == "INSUFFICIENT_EVIDENCE" and predicted is None
        annotation_required = prediction.get("annotationRequired", False)
        if not isinstance(annotation_required, bool):
            raise ValueError(f"Invalid annotation flag for {incident_id}")
        diagnostic = status in ("COMPLETED", "INSUFFICIENT_EVIDENCE") and not annotation_required
        correct = (predicted == expected if expected is not None else abstained) if diagnostic else None
        allowed = set(case["evidenceIds"])
        valid = sum(c in allowed for c in citations)
        supported = sum(c in set(case["causalEvidenceIds"]) for c in citations)
        tools = prediction.get("toolCalls")
        successes = prediction.get("successfulToolCalls")
        if tools is not None or successes is not None:
            if not (isinstance(tools, int) and not isinstance(tools, bool)
                    and isinstance(successes, int) and not isinstance(successes, bool)
                    and 0 <= successes <= tools):
                raise ValueError(f"Invalid tool counts for {incident_id}")
        rows.append({
            "incidentId": incident_id, "workflowId": prediction["workflowId"],
            "expected": case["expectedOutcome"], "status": status,
            "correct": correct, "abstained": abstained, "diagnostic": diagnostic,
            "annotationRequired": annotation_required,
            "expectedLabel": expected, "predictedLabel": predicted,
            "citationCount": len(citations), "validCitationCount": valid,
            "causalCitationCount": supported, "latencyMs": latency,
            "toolCalls": tools, "successfulToolCalls": successes,
        })

    supported_cases = [r for r in rows if r["expected"] == "SUPPORTED_CAUSE" and r["diagnostic"]]
    abstention_cases = [r for r in rows if r["expected"] == "INSUFFICIENT_EVIDENCE" and r["diagnostic"]]
    cited = sum(r["citationCount"] for r in rows if r["diagnostic"])
    tool_rows = [r for r in rows if r["toolCalls"] is not None]
    tool_total = sum(r["toolCalls"] for r in tool_rows)
    return {
        "summary": {
            "evaluated": len(rows), "goldTotal": len(gold),
            "diagnosticRuns": sum(r["diagnostic"] for r in rows),
            "technicalFailures": sum(r["status"] in ("FAILED", "TIMED_OUT", "CANCELLED") for r in rows),
            "pendingAnnotations": sum(r["annotationRequired"] for r in rows),
            "coverage": len(rows) / len(gold) if gold else None,
            "top1AccuracyOnSupportedCases": (
                sum(r["correct"] for r in supported_cases) / len(supported_cases)
                if supported_cases else None),
            "safeAbstentionRate": (
                sum(r["abstained"] for r in abstention_cases) / len(abstention_cases)
                if abstention_cases else None),
            "citationIdValidity": sum(r["validCitationCount"] for r in rows if r["diagnostic"]) / cited if cited else None,
            "causalEvidenceIdOverlap": sum(r["causalCitationCount"] for r in rows if r["diagnostic"]) / cited if cited else None,
            "toolCallSuccessRate": (
                sum(r["successfulToolCalls"] for r in tool_rows) / tool_total if tool_total else None),
            "medianLatencyMs": statistics.median(r["latencyMs"] for r in rows) if rows else None,
            "note": "Technical failures and annotation-required captures are excluded from diagnostic quality scores; citation ID overlap is NOT semantic support.",
        },
        "cases": rows,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--predictions", required=True, type=Path)
    parser.add_argument("--gold", type=Path, default=DEFAULT_GOLD)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    gold = json.loads(args.gold.read_text(encoding="utf-8"))["cases"]
    raw = json.loads(args.predictions.read_text(encoding="utf-8"))
    predictions = raw["predictions"] if isinstance(raw, dict) else raw
    if not isinstance(predictions, list):
        parser.error("Predictions must be an array")
    report = evaluate(predictions, gold)
    result = json.dumps(report, indent=2) + "\n"
    if args.output:
        args.output.write_text(result, encoding="utf-8")
    else:
        print(result, end="")


if __name__ == "__main__":
    main()
