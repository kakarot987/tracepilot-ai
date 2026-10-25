import unittest
from unittest.mock import patch
from capture_workflow import capture

class CaptureWorkflowTests(unittest.TestCase):
    def test_get_only_and_no_invented_citations(self):
        run = {"incidentId": "INC-CACHE-001", "status": "COMPLETED",
               "startedAt": "2026-10-01T00:00:00Z", "completedAt": "2026-10-01T00:00:01Z"}
        steps = [{"id": "step-1"}]
        output = {"investigationRunId": "run-1"}
        refs = [{"evidenceId": "LOG-CACHE-002"}]
        with patch("capture_workflow.get_json", side_effect=[run, steps, output, refs]) as getter:
            result = capture("http://localhost:8080", "workflow-1")
        self.assertEqual(4, getter.call_count)
        self.assertEqual(1000, result["latencyMs"])
        self.assertEqual(["LOG-CACHE-002"], result["retrievedEvidenceIds"])
        self.assertEqual([], result["citedEvidenceIds"])
        self.assertIsNone(result["predictedRootCause"])
        self.assertTrue(result["annotationRequired"])

    def test_refuses_nonterminal_workflow(self):
        with patch("capture_workflow.get_json", return_value={"incidentId": "INC-DB-001", "status": "RUNNING"}):
            with self.assertRaises(ValueError):
                capture("http://localhost:8080", "workflow-1")
