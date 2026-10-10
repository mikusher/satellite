"""Offline tests for release-only Dependabot alert gates."""
import unittest

from scripts.check_dependabot_alerts import assess_open_alerts


def alert(severity, state="open"):
    return {"state": state, "security_advisory": {"severity": severity}}


class DependabotAlertGateTest(unittest.TestCase):

    def test_empty_alerts_pass(self):
        ok, note = assess_open_alerts([])
        self.assertTrue(ok)
        self.assertIn("No open critical/high", note)

    def test_low_and_moderate_are_reported_but_do_not_block(self):
        ok, note = assess_open_alerts([alert("low"), alert("moderate")])
        self.assertTrue(ok)
        self.assertIn("moderate=1", note)

    def test_critical_and_high_block(self):
        for severity in ("critical", "high"):
            with self.subTest(severity=severity):
                ok, note = assess_open_alerts([alert(severity)])
                self.assertFalse(ok)
                self.assertIn("Unresolved", note)

    def test_missing_advisory_or_severity_fails_closed(self):
        for invalid in ({"state": "open"}, alert("unknown"), {}, None):
            with self.subTest(invalid=invalid):
                self.assertFalse(assess_open_alerts([invalid])[0])

    def test_bad_response_fails_closed(self):
        self.assertFalse(assess_open_alerts({"message": "not found"})[0])
        self.assertFalse(assess_open_alerts(None)[0])

    def test_pagination_uncertainty_fails_closed(self):
        self.assertFalse(assess_open_alerts([alert("low")] * 100)[0])

    def test_unexpected_closed_alert_fails_closed(self):
        self.assertFalse(assess_open_alerts([alert("low", "dismissed")])[0])


if __name__ == "__main__":
    unittest.main()
