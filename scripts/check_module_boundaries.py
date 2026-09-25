#!/usr/bin/env python3
"""Fail CI when Satellite module dependency boundaries are violated."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

DATA = "satellite-data"
DATA_COMPAT = "satellite-parametermap"
BRIDGE = "satellite-data-egress-bridge"
BRIDGE_COMPAT = "satellite-parametermap-egress-bridge"
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
        text = java_file.read_text(encoding="utf-8", errors="strict")
        for prefix in banned_prefixes:
            if f"import {prefix}" in text or f"package {prefix}" in text:
                errors.append(
                    f"{java_file.relative_to(ROOT)} crosses module boundary via {prefix}"
                )


# Satellite Data stays fully independent from Egress.
for group, artifact in dependencies(DATA):
    if group == "io.github.mikusher" and (
        artifact.startswith("satellite-egress-")
        or artifact in {BRIDGE, BRIDGE_COMPAT, LEGACY_LOGGING}
    ):
        errors.append(f"{DATA} must not depend on {group}:{artifact}")

scan_imports(DATA, ["io.github.mikusher.satellite.egress"])

# Egress never knows Satellite Data, compatibility artifacts or bridges.
for module in sorted(EGRESS_MODULES):
    for group, artifact in dependencies(module):
        if group == "io.github.mikusher" and artifact in {
            DATA,
            DATA_COMPAT,
            BRIDGE,
            BRIDGE_COMPAT,
            LEGACY_LOGGING,
        }:
            errors.append(f"{module} must not depend on {group}:{artifact}")

    scan_imports(module, ["com.mikusher.parameter", "com.mikusher.formats"])

# Legacy logging remains isolated from the new Egress product line.
for group, artifact in dependencies(LEGACY_LOGGING):
    if group == "io.github.mikusher" and (
        artifact.startswith("satellite-egress-")
        or artifact in {BRIDGE, BRIDGE_COMPAT}
    ):
        errors.append(f"{LEGACY_LOGGING} must not depend on {group}:{artifact}")

scan_imports(LEGACY_LOGGING, ["io.github.mikusher.satellite.egress"])

# The new bridge is the only primary module intentionally allowed to know both sides.
bridge_dependencies = set(dependencies(BRIDGE))
for required in {
    ("io.github.mikusher", DATA),
    ("io.github.mikusher", "satellite-egress-core"),
}:
    if required not in bridge_dependencies:
        errors.append(f"{BRIDGE} is missing required dependency {required[0]}:{required[1]}")

# Compatibility artifacts should only point forward to the new names.
if ("io.github.mikusher", DATA) not in set(dependencies(DATA_COMPAT)):
    errors.append(f"{DATA_COMPAT} must depend on {DATA}")

bridge_compat_dependencies = set(dependencies(BRIDGE_COMPAT))
if ("io.github.mikusher", BRIDGE) not in bridge_compat_dependencies:
    errors.append(f"{BRIDGE_COMPAT} must depend on {BRIDGE}")

if errors:
    print("Satellite architecture boundary violations:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    sys.exit(1)

print("Satellite module boundaries verified.")
