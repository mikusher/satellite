#!/usr/bin/env python3
"""Stage exactly the Satellite Maven coordinates in a local, non-public file repo.

This copies built artifacts from the CI runner's Maven cache. It neither runs
Maven deploy nor sends artifacts/credentials to a package registry.
"""
from __future__ import annotations

import argparse
import hashlib
import shutil
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
GROUP_PATH = Path("io/github/mikusher")


def artifacts_from_pom(root_pom: Path) -> list[str]:
    root = ET.parse(root_pom).getroot()
    parent = (root.findtext("m:artifactId", namespaces=NS) or "").strip()
    names = [parent]
    names.extend(
        (node.text or "").strip()
        for node in root.findall("./m:modules/m:module", NS)
    )
    if len(names) != 8 or len(set(names)) != len(names) or any(not n for n in names):
        raise ValueError("Expected one unique parent and seven unique Satellite modules")
    if any("/" in name or ".." in name for name in names):
        raise ValueError("Unsafe module identifier")
    return names


def stage(repo: Path, target: Path, version: str, root_pom: Path) -> list[str]:
    if not version or version.endswith("-SNAPSHOT") or "/" in version or ".." in version:
        raise ValueError("Staging requires a non-snapshot Maven candidate version")
    if target.resolve() == repo.resolve():
        raise ValueError("Source cache and staged repository must be distinct")

    artifacts = artifacts_from_pom(root_pom)
    required = []
    for artifact in artifacts:
        suffixes = (".pom",) if artifact == "satellite-parent" else (".pom", ".jar")
        for suffix in suffixes:
            relative = GROUP_PATH / artifact / version / (artifact + "-" + version + suffix)
            source = repo / relative
            if not source.is_file() or source.is_symlink():
                raise FileNotFoundError("Required candidate artifact missing: " + str(source))
            required.append(relative)

    report = []
    for relative in required:
        source = repo / relative
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        digest = hashlib.sha256(destination.read_bytes()).hexdigest()
        report.append(f"{digest}  {relative.as_posix()}")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--root-pom", default=Path("pom.xml"), type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()
    try:
        lines = stage(args.repository, args.output, args.version, args.root_pom)
    except (ValueError, FileNotFoundError, OSError, ET.ParseError) as error:
        print("Staging failed: " + str(error), file=sys.stderr)
        return 1
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Staged {len(lines)} artifacts from 7 modules + parent to local file repo")
    return 0


if __name__ == "__main__":
    sys.exit(main())
