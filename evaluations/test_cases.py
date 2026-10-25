"""Validate Milestone 6 offline evaluation cases against actual Flyway seed IDs.

Run from repository root: python -m unittest discover -s evaluations -p 'test_*.py'
This checks fixture integrity only; it does not score model accuracy.
"""
import json
import re
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASE_FILE = ROOT / "evaluations" / "milestone-6-cases.json"
INCIDENT_SQL = ROOT / "tracepilot-backend/src/main/resources/db/migration/V2__seed_synthetic_incidents.sql"
EVIDENCE_SQL = ROOT / "tracepilot-backend/src/main/resources/db/migration/V4__seed_log_evidence.sql"

# This parser deliberately extracts only the stable ID/service tuples at the
# start of each INSERT row, not arbitrary text from the log message.
INCIDENT_ROW = re.compile(r"\(\s*'(INC-[A-Z]+-\d+)',\s*'[^']*',\s*'[^']*',\s*'([^']+)'", re.MULTILINE)
LOG_ROW = re.compile(r"\(\s*'(LOG-[A-Z]+-\d+)',\s*'(INC-[A-Z]+-\d+)',\s*'([^']+)'", re.MULTILINE)


class EvaluationFixtureTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cases = json.loads(CASE_FILE.read_text(encoding="utf-8"))["cases"]
        cls.incidents = dict(INCIDENT_ROW.findall(INCIDENT_SQL.read_text(encoding="utf-8")))
        cls.evidence = {
            evidence_id: (incident_id, service)
            for evidence_id, incident_id, service in LOG_ROW.findall(EVIDENCE_SQL.read_text(encoding="utf-8"))
        }

    def test_exactly_twenty_unique_scenarios(self):
        self.assertEqual(20, len(self.cases))
        ids = [case["caseId"] for case in self.cases]
        self.assertEqual(len(ids), len(set(ids)))

    def test_all_references_are_seeded_and_scoped(self):
        for case in self.cases:
            with self.subTest(case=case["caseId"]):
                incident_id = case["incidentId"]
                self.assertIn(incident_id, self.incidents)
                self.assertEqual(self.incidents[incident_id], case["serviceName"])
                self.assertTrue(case["evidenceIds"])
                self.assertEqual(len(case["evidenceIds"]), len(set(case["evidenceIds"])))
                for evidence_id in case["evidenceIds"]:
                    self.assertIn(evidence_id, self.evidence)
                    self.assertEqual(self.evidence[evidence_id], (incident_id, case["serviceName"]))

    def test_abstentions_do_not_claim_proven_cause(self):
        outcomes = Counter(case["expectedOutcome"] for case in self.cases)
        self.assertEqual({"OBSERVED_FAILURE": 10, "INSUFFICIENT_EVIDENCE": 10}, dict(outcomes))
        for case in self.cases:
            with self.subTest(case=case["caseId"]):
                self.assertEqual("UNKNOWN_UNDERLYING_CAUSE", case["underlyingRootCause"])
                if case["expectedOutcome"] == "INSUFFICIENT_EVIDENCE":
                    self.assertIsNone(case["expectedObservedFailure"])
                else:
                    self.assertIsInstance(case["expectedObservedFailure"], str)
                    self.assertTrue(case["expectedObservedFailure"])


if __name__ == "__main__":
    unittest.main()
