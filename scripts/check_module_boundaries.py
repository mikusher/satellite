#!/usr/bin/env python3
"""Fail CI when Satellite module dependency boundaries are violated."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

DATA = "satellite-data"
BRIDGE = "satellite-data-egress-bridge"
LEGACY_LOGGING = "satellite-legacy-logging"
EGRESS_MODULES = {
    "satellite-egress-core",
    "satellite-egress-policy",
    "satellite-egress-observability",
    "satellite-egress-jackson",
    "satellite-egress-opentelemetry",
}

errors = []


def dependencies(module: str):
    pom = ROOT / module / "pom.xml"
    tree = ET.parse(pom)
    for dep in tree.findall(".//m:dependencies/m:dependency", NS):
        group = dep.findtext("m:groupId", default="", namespaces=NS)
        artifact = dep.findtext("m:artifactId", default="", namespaces=NS)
        yield group, artifact


def scan_imports(module: str, banned_prefixes):
    source_root = ROOT / module / "src"
    if not source_root.exists():
        return

    for java_file in source_root.rglob("*.java"):
        source = java_file.read_text(encoding="utf-8", errors="strict")
        for prefix in banned_prefixes:
            if f"import {prefix}" in source or f"package {prefix}" in source:
                errors.append(
                    f"{java_file.relative_to(ROOT)} crosses module boundary via {prefix}"
                )


for group, artifact in dependencies(DATA):
    if group == "io.github.mikusher" and (
        artifact.startswith("satellite-egress-")
        or artifact in {BRIDGE, LEGACY_LOGGING}
    ):
        errors.append(f"{DATA} must not depend on {group}:{artifact}")

scan_imports(DATA, ["io.github.mikusher.satellite.egress"])

for module in sorted(EGRESS_MODULES):
    for group, artifact in dependencies(module):
        if group == "io.github.mikusher" and artifact in {
            DATA,
            BRIDGE,
            LEGACY_LOGGING,
        }:
            errors.append(f"{module} must not depend on {group}:{artifact}")

    scan_imports(module, ["com.mikusher.parameter", "com.mikusher.formats"])

for group, artifact in dependencies(LEGACY_LOGGING):
    if group == "io.github.mikusher" and (
        artifact.startswith("satellite-egress-") or artifact == BRIDGE
    ):
        errors.append(f"{LEGACY_LOGGING} must not depend on {group}:{artifact}")

scan_imports(LEGACY_LOGGING, ["io.github.mikusher.satellite.egress"])

bridge_dependencies = set(dependencies(BRIDGE))
for required in {
    ("io.github.mikusher", DATA),
    ("io.github.mikusher", "satellite-egress-core"),
}:
    if required not in bridge_dependencies:
        errors.append(f"{BRIDGE} is missing required dependency {required[0]}:{required[1]}")

if errors:
    print("Satellite architecture boundary violations:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    sys.exit(1)

print("Satellite module boundaries verified.")
