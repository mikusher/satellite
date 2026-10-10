# Satellite 2 release readiness

Status: **not ready for stable 2.x publication**. This is a verification checklist, not a release announcement.

Assessment date: 2026-10-10. Baseline before this verification work: `master` at `16ae2f9a0b834a15d0493d4dfbf0a5a47aacc059`.

## Scope and non-goals

Validate what can be verified without modifying runtime behavior:
- Java 11 / 17 / 21 builds and regression suites;
- consuming all seven artifacts from an independent Maven project;
- realistic payment-policy, bridge, JSON, SLF4J preparation and OpenTelemetry attribute preparation;
- adversarial tests covering strict schema rejection, purpose separation, composites and unsafe custom transformations;
- reproducible, explicitly limited JMH baseline.

**Out of scope:** publish a release, change public APIs, deploy a production service, claim regulatory compliance, or infer that CodeQL success proves absence of vulnerabilities.

## Evidence and gates

| Gate | Status | Evidence / follow-up |
| --- | --- | --- |
| Seven-module reactor and dependency boundaries | Verified in baseline | `scripts/check_module_boundaries.py`; CI |
| Reactor tests on Java 11, 17 and 21 | Verified in baseline | GitHub Actions CI success on baseline SHA |
| Static analysis | Verified in baseline | CodeQL Java success on baseline SHA |
| Composite/unclassified value deny, redactor/tokenizer fail-closed | Existing regression tests | PR #45 merged; check `CompositeDataLeakTest`, `UnclassifiedCompositeEgressTest` and `TransformationOutputGuardrailTest` |
| External consumer compiles and tests Java 11 / 21 | **Verified on PR #46** | The standalone Maven project compiled and passed all four JUnit tests on Java 11 and 21 after correcting an inaccurate JSON expectation |
| Reproducible microbenchmark baseline | **Initial diagnostic run captured** | [JMH workflow run](https://github.com/mikusher/satellite/actions/runs/38090940750), Java 21, raw JSON artifact, see preliminary timings below |
| Live SLF4J backend / OpenTelemetry exporter / external HTTP integrations | **Not verified** | Current tests prepare approved output; require environment-backed contract tests before claiming end-to-end production validation |
| Version and artifact publication | **Blocked** | POM is `2.0.0-SNAPSHOT`, release workflow rejects snapshots; no published GitHub Release was found |
| Distribution channel | Identified, not published | `distributionManagement` points to GitHub Packages; do not claim Maven Central availability |
| Supply chain | Partially verified | SBOM/CodeQL/CI present; review open dependency update PRs and any alerts before GA |
| Versioned migration / compatibility / changelog | **Decision pending** | Choose release artifact coordinates, compatibility promise, SemVer/changelog and supported Java matrix |

## Reproduce the consumer gate

This project is intentionally outside the main reactor, so Maven must resolve **installed artifacts** instead of reactor project outputs.

```bash
mvn --batch-mode --no-transfer-progress verify
mvn --batch-mode --no-transfer-progress -DskipTests install
mvn --batch-mode --no-transfer-progress -f verification/pom.xml verify
```

The CI also runs the consumer project independently on Java 11 and 21. It tests an application workflow crossing Satellite Data → bridge → Egress policy → JSON/log/trace adapters and adversarial cases. No live API call, logger backend or tracing collector is required.

## Preliminary JMH baseline

An independent [JMH workflow run](https://github.com/mikusher/satellite/actions/runs/38090940750) succeeded on GitHub Actions (JMH 1.37, Java 21, 1 fork, 2 × 1-second warmup iterations and 3 × 1-second measurement iterations):

| Benchmark | Average time | Reported JMH error |
| --- | ---: | ---: |
| `publicLog` | 0.119 µs/op | ± 0.012 µs/op |
| `mixedLog` | 0.159 µs/op | ± 0.013 µs/op |
| `strictBridge` | 0.348 µs/op | ± 0.021 µs/op |

These are **preliminary CI-host measurements** for tiny synthetic data, not application performance or release SLOs. Re-run with more forks, realistic input sizes and comparable hardware before interpreting trends.

## Reproduce the benchmark

Follow [benchmarks/README.md](../benchmarks/README.md). The JMH workflow generates a JSON artifact and reports average **microseconds per operation** for a small test dataset.

A run in shared CI provides a *diagnostic baseline only*. It is not a throughput SLA, production performance measurement, or valid regression verdict against another machine. Do not set unstable CI microbenchmark numbers as automatic release thresholds.

## Criteria for a stable 2.x release

1. `mvn verify` green for Java 11, 17 and 21 on the exact release commit.
2. Independent consumer tests green on Java 11 and 21.
3. CodeQL and dependency/security reviews assessed with no unresolved release-blocking findings.
4. JMH baseline recorded on a known Java version/runner; investigation of any obvious anomalies.
5. Production-representative integration tests for intended logging provider/exporter and HTTP boundary, or explicit supported-scope limitations.
6. Version finalized (not `-SNAPSHOT`), release notes/change log, compatibility and supported-Java policy documented.
7. GitHub Packages credentials and publish workflow validated with a controlled release candidate; no Maven Central promise without a separate publishing workflow.
8. Explicit maintainer approval to publish.

## Current decision

**NO-GO for public stable release**, independently of tests going green, while the version remains a snapshot and production/distribution validations remain incomplete.

This report makes the uncertainty explicit. A clean independent consumer run and an initial JMH measurement do not remove the publication/version and live-integration blockers.
