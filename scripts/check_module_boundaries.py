#!/usr/bin/env python3
"""Fail CI when Satellite module dependency or naming boundaries are violated."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

DATA = "satellite-data"
BRIDGE = "satellite-data-egress-bridge"
EGRESS_MODULES = {
    "satellite-egress-core",
    "satellite-egress-policy",
    "satellite-egress-observability",
    "satellite-egress-jackson",
    "satellite-egress-opentelemetry",
}
EXPECTED_MODULES = {DATA, BRIDGE, *EGRESS_MODULES}

FORBIDDEN_LEGACY_NAMES = (
    "ParameterMap",
    "ParameterInfoMap",
    "SatelliteMap",
    "satellite-parametermap",
    "satellite-legacy-logging",
    "com.mikusher.logger",
)

SCAN_SUFFIXES = {
    ".java",
    ".md",
    ".xml",
    ".yml",
    ".yaml",
    ".py",
    ".properties",
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


root_tree = ET.parse(ROOT / "pom.xml")
reactor_modules = {
    module.text.strip()
    for module in root_tree.findall("./m:modules/m:module", NS)
    if module.text and module.text.strip()
}

if reactor_modules != EXPECTED_MODULES:
    missing = sorted(EXPECTED_MODULES - reactor_modules)
    unexpected = sorted(reactor_modules - EXPECTED_MODULES)
    if missing:
        errors.append(f"root reactor is missing modules: {', '.join(missing)}")
    if unexpected:
        errors.append(f"root reactor contains unexpected modules: {', '.join(unexpected)}")

for group, artifact in dependencies(DATA):
    if group == "io.github.mikusher" and (
        artifact.startswith("satellite-egress-") or artifact == BRIDGE
    ):
        errors.append(f"{DATA} must not depend on {group}:{artifact}")

scan_imports(DATA, ["io.github.mikusher.satellite.egress"])

for module in sorted(EGRESS_MODULES):
    for group, artifact in dependencies(module):
        if group == "io.github.mikusher" and artifact in {DATA, BRIDGE}:
            errors.append(f"{module} must not depend on {group}:{artifact}")

    scan_imports(module, ["com.mikusher.parameter", "com.mikusher.formats"])

bridge_dependencies = set(dependencies(BRIDGE))
for required in {
    ("io.github.mikusher", DATA),
    ("io.github.mikusher", "satellite-egress-core"),
}:
    if required not in bridge_dependencies:
        errors.append(
            f"{BRIDGE} is missing required dependency {required[0]}:{required[1]}"
        )

# Satellite 2 is pre-release: removed public naming must not reappear.
for candidate in ROOT.rglob("*"):
    if not candidate.is_file():
        continue
    if any(part in {".git", "target"} for part in candidate.parts):
        continue
    if candidate.suffix not in SCAN_SUFFIXES and candidate.name != "pom.xml":
        continue

    source = candidate.read_text(encoding="utf-8", errors="strict")
    relative = candidate.relative_to(ROOT)

    # This enforcement script contains the forbidden tokens by definition.
    if relative == Path("scripts/check_module_boundaries.py"):
        continue

    for legacy_name in FORBIDDEN_LEGACY_NAMES:
        if legacy_name.lower() in source.lower():
            errors.append(
                f"{relative} still contains removed Satellite 2 name: {legacy_name}"
            )

if errors:
    print("Satellite architecture boundary violations:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    sys.exit(1)

print("Satellite module boundaries and naming verified.")
