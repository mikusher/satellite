"""Verify local file repository staging without accessing package services."""
import tempfile
import unittest
from pathlib import Path

from scripts.stage_local_maven_repo import GROUP_PATH, stage

MODULES = [
    "satellite-data",
    "satellite-egress-core",
    "satellite-egress-policy",
    "satellite-egress-observability",
    "satellite-egress-jackson",
    "satellite-egress-opentelemetry",
    "satellite-data-egress-bridge",
]


class LocalMavenStagingTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.repo = self.root / "cache"
        self.target = self.root / "distribution"
        self.version = "2.0.0-rc.1"
        self.pom = self.root / "pom.xml"
        self.pom.write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            '<artifactId>satellite-parent</artifactId>'
            '<modules>'
            + "".join(f"<module>{name}</module>" for name in MODULES)
            + '</modules></project>', encoding="utf-8"
        )
        for artifact in ["satellite-parent"] + MODULES:
            suffixes = [".pom"] if artifact == "satellite-parent" else [".pom", ".jar"]
            for suffix in suffixes:
                file = self.repo / GROUP_PATH / artifact / self.version / (
                    artifact + "-" + self.version + suffix
                )
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_bytes((artifact + suffix).encode("utf-8"))

    def test_all_parent_and_module_artifacts_are_copied(self):
        manifest = stage(self.repo, self.target, self.version, self.pom)
        self.assertEqual(15, len(manifest))
        for artifact in ["satellite-parent"] + MODULES:
            pom = self.target / GROUP_PATH / artifact / self.version / (
                artifact + "-" + self.version + ".pom"
            )
            self.assertTrue(pom.is_file())
        self.assertTrue(all(len(line.split("  ")[0]) == 64 for line in manifest))

    def test_missing_artifact_rejects_without_partial_staging(self):
        missing = self.repo / GROUP_PATH / MODULES[0] / self.version / (
            MODULES[0] + "-" + self.version + ".jar"
        )
        missing.unlink()
        with self.assertRaises(FileNotFoundError):
            stage(self.repo, self.target, self.version, self.pom)
        self.assertFalse(self.target.exists())

    def test_snapshots_and_same_directory_are_rejected(self):
        with self.assertRaises(ValueError):
            stage(self.repo, self.target, "2.0.0-SNAPSHOT", self.pom)
        with self.assertRaises(ValueError):
            stage(self.repo, self.repo, self.version, self.pom)


if __name__ == "__main__":
    unittest.main()
