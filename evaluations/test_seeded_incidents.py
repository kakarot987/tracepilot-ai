"""Verify 20 distinct seeded incidents, scoped logs and independent gold labels."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "tracepilot-backend/src/main/resources/db/migration"
GOLD = ROOT / "evaluations/milestone-6-incident-ground-truth.json"

INCIDENT = re.compile(r"\(\s*'(INC-[A-Z]+-\d+)',\s*'[^']*',\s*'[^']*',\s*'([^']+)'", re.MULTILINE)
LOG = re.compile(r"\(\s*'(LOG-[A-Z]+-\d+)',\s*'(INC-[A-Z]+-\d+)',\s*'([^']+)'", re.MULTILINE)


class SeededIncidentGroundTruthTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        incident_sql = "\n".join(
            (MIGRATIONS / file).read_text(encoding="utf-8")
            for file in ("V2__seed_synthetic_incidents.sql", "V17__seed_evaluation_incidents.sql")
        )
        log_sql = "\n".join(
            (MIGRATIONS / file).read_text(encoding="utf-8")
            for file in ("V4__seed_log_evidence.sql", "V17__seed_evaluation_incidents.sql")
        )
        incident_rows = INCIDENT.findall(incident_sql)
        log_rows = LOG.findall(log_sql)
        cls.incidents = dict(incident_rows)
        cls.logs = {log_id: (incident_id, service) for log_id, incident_id, service in log_rows}
        cls.cases = json.loads(GOLD.read_text(encoding="utf-8"))["cases"]
        cls.incident_count = len(incident_rows)
        cls.log_count = len(log_rows)

    def test_twenty_unique_seeded_incidents(self):
        self.assertEqual(20, self.incident_count)
        self.assertEqual(20, len(self.incidents))
        self.assertEqual(60, self.log_count)
        self.assertEqual(60, len(self.logs))

    def test_every_incident_has_ground_truth_and_three_scoped_logs(self):
        self.assertEqual(20, len(self.cases))
        self.assertEqual(set(self.incidents), {case["incidentId"] for case in self.cases})
        self.assertEqual(20, len({case["caseId"] for case in self.cases}))
        for case in self.cases:
            with self.subTest(case=case["caseId"]):
                self.assertEqual(self.incidents[case["incidentId"]], case["serviceName"])
                self.assertEqual(3, len(case["evidenceIds"]))
                for evidence_id in case["evidenceIds"]:
                    self.assertEqual(
                        (case["incidentId"], case["serviceName"]), self.logs[evidence_id]
                    )
                if case["expectedOutcome"] == "INSUFFICIENT_EVIDENCE":
                    self.assertIsNone(case["rootCauseLabel"])
                    self.assertEqual([], case["causalEvidenceIds"])
                else:
                    self.assertEqual("SUPPORTED_CAUSE", case["expectedOutcome"])
                    self.assertTrue(case["rootCauseLabel"])
                    self.assertTrue(set(case["causalEvidenceIds"]).issubset(case["evidenceIds"]))

    def test_both_grounded_and_abstention_cases_exist(self):
        outcomes = {outcome: sum(case["expectedOutcome"] == outcome for case in self.cases)
                    for outcome in ("SUPPORTED_CAUSE", "INSUFFICIENT_EVIDENCE")}
        self.assertEqual({"SUPPORTED_CAUSE": 12, "INSUFFICIENT_EVIDENCE": 8}, outcomes)


if __name__ == "__main__":
    unittest.main()
