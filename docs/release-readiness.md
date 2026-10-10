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
| Logback / OpenTelemetry SDK / local HTTP boundaries | **Verified in PR #47 CI** | Independent Java 11/21 tests capture Logback events, SDK-exported spans and real HTTP requests to a loopback server; remote collectors and production gateways remain unverified |
| Version and artifact publication | **Blocked intentionally** | POM is `2.0.0-SNAPSHOT` and no GitHub Release is published; release workflow now checks the Git tag against every module POM and has no manual publish dispatch |
| Local Maven file-repository consumer smoke | **Verified on PR #48** | [Candidate workflow run 38092753055](https://github.com/mikusher/satellite/actions/runs/38092753055) completed successfully; separate clean Maven consumer resolved all staged artifacts, and SHA-256 inventory was uploaded | 
| Distribution channel | Identified, not published | `distributionManagement` points to GitHub Packages; do not claim Maven Central availability |
| Supply chain | **Blocked: Dependency Graph unavailable** | PR #50 Dependency Review job returned success **but skipped the actual review step**: SBOM availability check returned HTTP 404. Java library patches were merged in #49 and tested by the other gates; that does **not** establish vulnerability-review coverage. Enable the graph/alerts and pass the separate release security preflight. |
| CodeQL/checkout Actions maintenance | **Verified on PR #51** | `actions/checkout@v7` and `actions/setup-java@v6` updated in CodeQL; CodeQL passed. Old Dependabot PRs #23/#30 closed as superseded. The Dependency Review workflow now states clearly when its actual scan is skipped. | 
| Versioned migration / compatibility / changelog | **Documented procedure; decision pending** | [Controlled release process](release-process.md) describes RC and tag checks; GA version, compatibility promise and changelog still require approval |

## Live-boundary verification (PR #47)

The independent consumer has three additional integration tests executed in CI on Java 11 and 21:

- **Logback:** records a real backend `ILoggingEvent` through `Slf4jEgressLogger.info(...)` and asserts that email and credentials do not appear raw.
- **OpenTelemetry SDK:** ends a real span and inspects `SpanData` emitted by an `InMemorySpanExporter`; the confidential value is redacted and the credential absent.
- **Loopback HTTP:** uses Java 11 `HttpClient` to deliver a JSON payload to an actual server bound to `127.0.0.1`. The payment-provider purpose permits explicitly authorized email/token fields; the analytics purpose receives only the public order ID.

These results verify actual library-to-backend interactions. They **do not prove** behaviour with a production log aggregator, remote OTLP collector or external payment endpoint. Satellite requires callers to pass policy-approved output to their HTTP clients; it does not intercept arbitrary network calls.

## Release candidate dry run (PR #47)

A separate workflow stages **`2.0.0-rc.1` inside the ephemeral CI checkout** and verifies:

1. root/module Maven coordinates are updated together and match the expected Git tag;
2. release-tag validation and its negative/positive unit tests;
3. complete candidate `mvn verify` and local `mvn install`;
4. independent consumer tests using `-Dsatellite.version=2.0.0-rc.1`.

**The dry-run CI job passed.** It never executes `mvn deploy` or creates a GitHub Release. GitHub Packages upload credentials and fetch from a fresh authenticated machine remain release-candidate gates.

## Isolated distribution verification

The candidate workflow now stages the eight Maven coordinates (one parent POM and seven module POM/JAR pairs, **15 files**) into a temporary **file-based Maven repository** with a SHA-256 inventory. The independent consumer is then tested with a completely fresh local Maven cache, resolving Satellite coordinates through the staged file repository rather than the reactor or the original runner cache.

This verification is safer and more realistic than testing only against locally installed packages. However, it **does not** test publishing to GitHub Packages, GitHub authorization, Maven Central availability or retrieval from a remote registry.

The new check passed on PR #48, including the independent Java consumer tests. The [staging SHA-256 inventory](https://github.com/mikusher/satellite/actions/runs/38092753055) is available as a workflow artifact.

## Reproduce the consumer gate

This project is intentionally outside the main reactor, so Maven must resolve **installed artifacts** instead of reactor project outputs.

```bash
mvn --batch-mode --no-transfer-progress verify
mvn --batch-mode --no-transfer-progress -DskipTests install
mvn --batch-mode --no-transfer-progress -f verification/pom.xml verify
```

The CI runs the consumer project independently on Java 11 and 21, including Satellite Data → bridge → Egress policy → JSON/log/trace adapters, adversarial cases, real Logback backend events, an in-memory OpenTelemetry SDK exporter and loopback HTTP. No externally hosted service or production credential is required.

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
3. CodeQL green **and** the reusable Release security preflight green on the candidate commit. Maven publication now depends on this security job automatically. Dependency Graph and accessible Dependabot alerts must be available, and unresolved high/critical alerts must be addressed. A green skipped Dependency Review job is insufficient.
4. JMH baseline recorded on a known Java version/runner; investigation of any obvious anomalies.
5. Verified Logback, SDK-exporter and loopback HTTP integration plus explicit limits for production aggregators, remote OTLP collectors and external endpoints.
6. Version finalized (not `-SNAPSHOT`), release notes/changelog, compatibility and supported-Java policy documented.
7. Candidate dry run green; GitHub Packages credentials and authenticated artifact fetch still need a controlled, approved release candidate; no Maven Central promise without a separate publishing workflow.
8. Explicit maintainer approval to publish.

## Draft release notes

A provisional, unpublished list of Satellite 2.x changes is maintained in [CHANGELOG.md](../CHANGELOG.md). It is not a release tag, a publishing approval or a stability/compatibility commitment.

## Dependency Graph coverage gap

On [PR #50](https://github.com/mikusher/satellite/pull/50), the `Dependency Review` workflow displayed a green job, but its `Review dependency changes` step was **skipped** because the preliminary GitHub SBOM request returned **HTTP 404**. This is not a completed dependency vulnerability review.

A separate [Release security preflight](../.github/workflows/release-security-preflight.yml) is available **manually** and is **automatically required before Maven publication**. It is fail-closed. It requires a successful Dependency Graph request and access to open Dependabot alerts; unknown API responses or critical/high alerts block release. The workflow has no publish step and is not a required development PR check.

Before a release, enable/configure **Dependency Graph** and **Dependabot alerts** in the repository's code security settings, ensure the workflow token has access, rerun the preflight on the candidate commit, and review medium/low alerts individually. There is no authenticated alert evidence yet.

## Current decision

**NO-GO for public stable release**, independently of tests going green, while the version remains a snapshot and production/distribution validations remain incomplete.

This report makes the uncertainty explicit. The new live-boundary integrations and candidate dry run remove some testing gaps, but do **not** remove publication/version approval, real external-infrastructure and artifact-distribution blockers.
