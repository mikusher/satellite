# Proposed Satellite 2.0.0-rc.1 — review only

**Status: DRAFT / NOT APPROVED / NOT PUBLISHED.**

This document accompanies an **unmerged draft pull request** proposing a consistent
`2.0.0-rc.1` Maven version. It is not a release announcement, a final API freeze or
authorization to publish a GitHub Release.

## Proposed artifacts

Group: `io.github.mikusher`

- `satellite-parent` (POM only)
- `satellite-data`
- `satellite-egress-core`
- `satellite-egress-policy`
- `satellite-egress-observability`
- `satellite-egress-jackson`
- `satellite-egress-opentelemetry`
- `satellite-data-egress-bridge`

Each artifact's version is `2.0.0-rc.1`. The standalone `verification` and
`benchmarks` projects retain their independent project versions but point their
`satellite.version` property to the candidate to exercise the correct coordinates.

## Supported runtime proposal

Java **11** is the bytecode/compiler baseline. CI currently exercises Java 11,
17 and 21; consumers compile and test independently on Java 11 and 21.

These are the tested runtimes, **not** a guarantee that every integration or
third-party exporter works in every application environment.

The candidate is **pre-stable**: breaking API changes remain possible before GA.
Once an eventual stable release is approved, the maintainer should decide and
document the SemVer/source/binary compatibility commitment.

## Content of the candidate

The draft [CHANGELOG](../CHANGELOG.md) describes the intended scope:
typed dynamic Satellite Data, classified Egress envelopes, purpose-scoped
policy enforcement, fail-closed defaults, safe logging/serialization/telemetry
adapters, the optional strict bridge and parser hardening.

Do not treat draft claims as independently audited security guarantees.

## Validation before approval

- Java 11/17/21 reactor CI on the exact proposed commit.
- Java 11/21 independent Maven consumer and live-boundary tests.
- CodeQL and candidate packaging dry run, including a fresh Maven-cache consumer.
- Inspect the changed Maven coordinates and all dependency transitive versions.
- Actual Dependency Graph / Dependabot alert inspection via the fail-closed
  **Release security preflight**. The most recently recorded SBOM endpoint result
  was HTTP **404**. A green skipped Dependency Review job is insufficient.
- Revisit unverified external scanner action updates before claiming scanner coverage.
- Review code security alerts, external infrastructure validation boundaries,
  and the application-facing migration guidance.
- Final review of version, tag, release notes, GitHub Packages availability,
  package access rights and authentication behavior.

## Publication safeguards

**No step in this draft proposal creates a tag, publishes packages, or launches a
GitHub Release.** Only the maintainer can subsequently authorize a candidate
publication, after the security gate is genuinely available and green.

When approved, follow [release-process.md](release-process.md) and use tag
`v2.0.0-rc.1` on the reviewed release commit. Publishing is configured for
GitHub Packages, not Maven Central.

If repository security configuration remains inaccessible, the release remains
**NO-GO**, regardless of normal CI success.
