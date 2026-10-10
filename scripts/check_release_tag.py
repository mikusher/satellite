#!/usr/bin/env python3
"""Validate a GitHub release tag and every Maven module version before publication.

Read-only preflight. This script never modifies files or publishes artifacts.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
VERSION_PATTERN = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:-rc\.[1-9][0-9]*)?$")


def maven_version(root: ET.Element) -> str:
    return (root.findtext("m:version", namespaces=NS) or "").strip()


def validate_release(tag: str, root_pom: Path) -> str:
    """Return release version or raise ValueError if the tag/reactor does not match."""
    if not tag or not tag.startswith("v"):
        raise ValueError("Git release tag must begin with 'v'")

    version = tag[1:]
    if not VERSION_PATTERN.fullmatch(version):
        raise ValueError(
            "Release tag must be vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-rc.N"
        )

    pom_root = ET.parse(root_pom).getroot()
    actual = maven_version(pom_root)
    if actual != version:
        raise ValueError(
            f"Release tag {tag} does not match root POM version {actual}"
        )

    modules = [
        (node.text or "").strip()
        for node in pom_root.findall("./m:modules/m:module", NS)
    ]
    if not modules or any(not module for module in modules):
        raise ValueError("Expected a nonempty Maven module list")

    for module in modules:
        if module.startswith("/") or ".." in Path(module).parts:
            raise ValueError(f"Unsafe module path: {module}")

        module_pom = root_pom.parent / module / "pom.xml"
        child_root = ET.parse(module_pom).getroot()
        parent_version = (
            child_root.findtext("./m:parent/m:version", namespaces=NS) or ""
        ).strip()
        if parent_version != version:
            raise ValueError(
                f"Module {module} parent version {parent_version} != {version}"
            )

    return version


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag", help="The Git tag associated with a published release")
    parser.add_argument("--pom", type=Path, default=Path("pom.xml"))
    arguments = parser.parse_args()

    try:
        version = validate_release(arguments.tag, arguments.pom)
    except (ValueError, OSError, ET.ParseError) as failure:
        print(f"Release preflight rejected: {failure}", file=sys.stderr)
        return 1

    print(f"Release preflight accepted: {version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
