#!/usr/bin/env python3
"""Fail a release security gate on unavailable or high/critical open Dependabot alerts.

Input must be the GitHub REST "open alerts" response with per_page=100.
This script never contacts GitHub or prints advisory details.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def assess_open_alerts(data: object) -> tuple[bool, str]:
    if not isinstance(data, list):
        return False, "Invalid Dependabot alerts response (expected JSON array)"
    if len(data) >= 100:
        return False, "100+ open alerts: pagination not audited; release gate fails closed"

    counts = {"critical": 0, "high": 0, "moderate": 0, "low": 0}
    for entry in data:
        if not isinstance(entry, dict) or entry.get("state") != "open":
            return False, "Expected only open Dependabot alerts"
        advisory = entry.get("security_advisory")
        if not isinstance(advisory, dict):
            return False, "Missing security advisory in Dependabot alert"
        severity = advisory.get("severity")
        if severity not in counts:
            return False, "Unknown/missing severity in Dependabot alert"
        counts[severity] += 1

    summary = ", ".join(f"{level}={n}" for level, n in counts.items())
    if counts["critical"] or counts["high"]:
        return False, f"Unresolved critical/high alerts: {summary}"
    return True, f"No open critical/high alerts among {len(data)} reviewed: {summary}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", required=True, type=Path)
    args = parser.parse_args()

    try:
        with args.file.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except (OSError, ValueError) as exc:
        print(f"Cannot read Dependabot alert evidence: {exc}", file=sys.stderr)
        return 1

    ok, reason = assess_open_alerts(data)
    print(reason, file=sys.stdout if ok else sys.stderr)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
