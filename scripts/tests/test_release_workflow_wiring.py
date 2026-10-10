"""Guard against removing the release security dependency from Maven publishing.

This is a static wiring regression check; it does not execute a GitHub Release or
claim that external vulnerability APIs are available.
"""
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class ReleaseSecurityWiringTest(unittest.TestCase):

    def test_publisher_waits_for_security_check(self):
        content = (ROOT / ".github/workflows/maven-publish.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("uses: ./.github/workflows/release-security-preflight.yml", content)
        self.assertIn("needs: security", content)
        self.assertIn("RELEASE_SECURITY_TOKEN: ${{ secrets.RELEASE_SECURITY_TOKEN }}", content)
        self.assertNotIn("workflow_dispatch:", content)

    def test_reusable_preflight_keeps_manual_mode_and_permissions(self):
        content = (ROOT / ".github/workflows/release-security-preflight.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("workflow_call:", content)
        self.assertIn("workflow_dispatch:", content)
        self.assertIn("security-events: read", content)
        self.assertIn("scripts/check_dependabot_alerts.py", content)
        self.assertNotIn("mvn deploy", content)


if __name__ == "__main__":
    unittest.main()
