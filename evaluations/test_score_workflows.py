import unittest
from score_workflows import evaluate

GOLD = [
    {"incidentId": "INC-A", "expectedOutcome": "SUPPORTED_CAUSE", "rootCauseLabel": "CONFIG_ERROR", "evidenceIds": ["LOG-A"], "causalEvidenceIds": ["LOG-A"]},
    {"incidentId": "INC-B", "expectedOutcome": "INSUFFICIENT_EVIDENCE", "rootCauseLabel": None, "evidenceIds": ["LOG-B"], "causalEvidenceIds": []},
]


class WorkflowScoringTests(unittest.TestCase):
    def test_scores_supported_cause_and_safe_abstention(self):
        data = [
            {"incidentId": "INC-A", "workflowId": "run-a", "status": "COMPLETED", "latencyMs": 500, "predictedRootCause": "CONFIG_ERROR", "citedEvidenceIds": ["LOG-A"], "toolCalls": 2, "successfulToolCalls": 2},
            {"incidentId": "INC-B", "workflowId": "run-b", "status": "INSUFFICIENT_EVIDENCE", "latencyMs": 1000, "predictedRootCause": None, "citedEvidenceIds": []},
        ]
        result = evaluate(data, GOLD)["summary"]
        self.assertEqual(2, result["evaluated"])
        self.assertEqual(1.0, result["top1AccuracyOnSupportedCases"])
        self.assertEqual(1.0, result["safeAbstentionRate"])
        self.assertEqual(1.0, result["citationIdValidity"])
        self.assertEqual(1.0, result["toolCallSuccessRate"])
        self.assertEqual(750, result["medianLatencyMs"])

    def test_invalid_and_duplicate_citations_are_not_accepted(self):
        row = {"incidentId": "INC-A", "workflowId": "run-a", "status": "COMPLETED", "latencyMs": 1, "predictedRootCause": "CONFIG_ERROR", "citedEvidenceIds": ["OTHER"]}
        self.assertEqual(0.0, evaluate([row], GOLD)["summary"]["citationIdValidity"])
        row["citedEvidenceIds"] = ["LOG-A", "LOG-A"]
        with self.assertRaises(ValueError):
            evaluate([row], GOLD)

    def test_duplicate_runs_are_rejected(self):
        row = {"incidentId": "INC-A", "workflowId": "run-a", "status": "FAILED", "latencyMs": 10, "predictedRootCause": None, "citedEvidenceIds": []}
        with self.assertRaises(ValueError):
            evaluate([row, row], GOLD)

    def test_no_citations_or_tool_data_yields_null_not_perfect_score(self):
        row = {"incidentId": "INC-B", "workflowId": "run-b", "status": "INSUFFICIENT_EVIDENCE", "latencyMs": 10, "predictedRootCause": None, "citedEvidenceIds": []}
        result = evaluate([row], GOLD)["summary"]
        self.assertIsNone(result["citationIdValidity"])
        self.assertIsNone(result["toolCallSuccessRate"])
        self.assertIsNone(result["top1AccuracyOnSupportedCases"])


if __name__ == "__main__":
    unittest.main()
