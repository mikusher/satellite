# Satellite 2 — controlled release process

Satellite 2 is currently at `2.0.0-SNAPSHOT`. **No release has been approved or published by this work.**

The artifacts are configured for **GitHub Packages** (`https://maven.pkg.github.com/mikusher/satellite`). There is no Maven Central publication workflow and no Maven Central availability guarantee.

## Release gates

A stable release must meet the conditions in [release readiness](release-readiness.md), including:

1. Reactor verification on Java 11 / 17 / 21.
2. Independent consumer integration tests on Java 11 / 21, including live Logback events, SDK-exported spans and loopback HTTP.
3. CodeQL and dependency review, with any release-blocking vulnerabilities resolved.
4. Candidate JMH results retained as diagnostic evidence, not an SLA.
5. Finalized Java compatibility, version, changelog and artifacts.
6. Explicit maintainer approval **before** creating/publishing a GitHub Release.

## Version and tag contract

The Maven publishing workflow runs **only** when a GitHub Release is published. Manual workflow dispatch was intentionally removed.

Before deploying, it runs `scripts/check_release_tag.py` to require:

- a Git tag in `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-rc.N` format;
- the root Maven POM version equal to the tag without its `v` prefix;
- every reactor module's parent version equal to the root version;
- no `-SNAPSHOT` version.

A mismatched tag or module version fails the job **before publication**.

## Rehearse without publishing

The GitHub Actions workflow [Release candidate dry run](../.github/workflows/release-candidate-check.yml) is safe to run manually and is also triggered by changes to release-related files.

It operates only in the temporary CI checkout:

```text
2.0.0-SNAPSHOT source
       |
       v
set temporary candidate version 2.0.0-rc.1
       |
       v
check Git tag / POM consistency
       |
       v
mvn verify
       |
       v
mvn install (local CI cache only)
       |
       v
independent consumer tests against candidate artifacts
```

**No `deploy` and no GitHub Release creation occur in the dry-run workflow.** It never changes `master` and does not create a permanent Git tag.

The rehearsal also copies exactly the parent POM and all seven module POM/JAR pairs into an ephemeral **local file-based Maven repository**, then runs the full independent consumer test suite using a **fresh Maven cache**. This catches missing artifact coordinates and transitive parent/dependency resolution that a same-cache `mvn install` can conceal.

A SHA-256 inventory of the staged artifacts is retained as a GitHub Actions artifact. The staging script never runs `mvn deploy`, accesses GitHub Packages, or publishes a Git tag.

This verifies local Maven-repository resolution, **not** upload permissions, registry-side distribution, or authentication to GitHub Packages. Those still need a controlled, approved release candidate.

## Release candidate steps — future, not executed

When all gates are approved:

1. Create a dedicated release PR for the agreed version (for example `2.0.0-rc.1`) and update **all seven module parent versions** together with the root POM.
2. Review CHANGELOG/release notes and Java compatibility support.
3. Merge the versioned release PR only after required checks pass.
4. Create the matching tag (for example `v2.0.0-rc.1`) on that exact commit.
5. With explicit maintainer approval, publish the corresponding GitHub Release and monitor the Maven Package workflow.
6. Verify that the expected seven artifacts can be fetched from GitHub Packages in a fresh authenticated environment.
7. For GA, repeat with the approved stable version and tag.

The availability of a CI dry run does not authorize automatic publication.

## Boundary and risk assumptions

- Satellite **enforces policies on envelopes passed through its API**; it does not intercept arbitrary HTTP clients or third-party logging calls.
- The HTTP integration test uses the loopback interface and forwards only `EgressReport.getOutput()`.
- The OpenTelemetry test verifies an actual SDK export into an in-memory exporter, **not** a remotely running collector.
- The SLF4J test verifies a real Logback backend receiving the approved message, not a production configuration with log aggregation.
- JMH numbers from shared GitHub runners are diagnostic only.
- Dependabot PRs for libraries and GitHub Actions must be reviewed individually. No automatic upgrades are implied by release readiness.

## Supported target

Java 11 is the compiler baseline, with reactor CI on Java 11, 17 and 21. The project is still pre-stable; a final public compatibility commitment belongs in the GA release notes.
