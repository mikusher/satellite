"""Preflight contract tests for release tags and the Maven reactor."""

import tempfile
import unittest
from pathlib import Path

from scripts.check_release_tag import validate_release


class ReleaseTagTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.write_reactor("2.0.0-rc.1")

    def write_reactor(self, version, child_version=None):
        child_version = child_version or version
        (self.root / "pom.xml").write_text(
            '<?xml version="1.0"?><project xmlns="http://maven.apache.org/POM/4.0.0">'
            f"<version>{version}</version>"
            "<modules><module>satellite-data</module></modules>"
            "</project>",
            encoding="utf-8",
        )
        module = self.root / "satellite-data"
        module.mkdir(exist_ok=True)
        (module / "pom.xml").write_text(
            '<?xml version="1.0"?><project xmlns="http://maven.apache.org/POM/4.0.0">'
            f"<parent><version>{child_version}</version></parent>"
            "</project>",
            encoding="utf-8",
        )

    def test_matching_release_candidate_passes(self):
        self.assertEqual(
            "2.0.0-rc.1", validate_release("v2.0.0-rc.1", self.root / "pom.xml")
        )

    def test_matching_stable_version_passes(self):
        self.write_reactor("2.0.0")
        self.assertEqual("2.0.0", validate_release("v2.0.0", self.root / "pom.xml"))

    def test_snapshot_tag_is_rejected(self):
        self.write_reactor("2.0.0-SNAPSHOT")
        with self.assertRaises(ValueError):
            validate_release("v2.0.0-SNAPSHOT", self.root / "pom.xml")

    def test_mismatched_tag_is_rejected(self):
        with self.assertRaises(ValueError):
            validate_release("v2.0.0", self.root / "pom.xml")

    def test_mismatched_module_is_rejected(self):
        self.write_reactor("2.0.0-rc.1", child_version="2.0.0-SNAPSHOT")
        with self.assertRaises(ValueError):
            validate_release("v2.0.0-rc.1", self.root / "pom.xml")

    def test_non_release_tags_are_rejected(self):
        for tag in ("2.0.0", "main", "v2.0.0-beta", "v1.0.0-rc.0", "v2.0"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                validate_release(tag, self.root / "pom.xml")


if __name__ == "__main__":
    unittest.main()
