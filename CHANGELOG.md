# Changelog

Changes intended for Satellite 2.x. **Unreleased / draft:** this document is not a version tag, compatibility guarantee, release announcement or publishing approval.

## [Unreleased] — Satellite 2.x

### Added

- Typed dynamic application data (`SatelliteData`) and optional structural definitions (`DataDefinition`).
- Classified `Key<T>` values, runtime origin/trust metadata, immutable `EgressEnvelope`, schema validation, and policy-controlled `EgressReport`.
- Rules for `ALLOW`, `REDACT`, `TOKENIZE` and `DENY`, with secure defaults and explicit sink/purpose scoping.
- Optional SLF4J, Jackson and OpenTelemetry adapters that process egress policies before external output.
- Strict schema-aware bridge from `SatelliteData` to `EgressEnvelope`.
- Getting Started examples with expected output and separate policy, security, architecture and release documentation.

### Improved

- Concise API shortcuts: `EgressProcessor.secureDefaults()`, `process(envelope, sink, purpose)`, `EgressRules` conveniences, `withSecureDefaults()` and secure adapter factories.
- PMAP/XML parser hardening and refactoring; bounded parsing and rejection of DTD/external entity usage.
- Clearer failure handling and reduced legacy compatibility surface in Satellite Data.
- Build and verification on Java 11, 17 and 21, with independent external-consumer tests on Java 11/21.
- Release-candidate *dry-run*, local staged Maven repository with clean consumer resolution, and JMH diagnostic baseline.
- Three patch dependency updates (Jackson 2.22.3, Commons Lang 3.21.0, Guava 33.7.2-jre) in PR #49.

### Security guarantees under test

- Unmatched policies fail closed.
- Unclassified nested composite values are denied, including when wrapped in publicly classified fields.
- Confidential data cannot be emitted raw into observability; credential/secret default egress is denied.
- Failed, empty or raw-echo redaction/tokenization outputs are denied.
- Strict bridge rejects unclassified/missing required fields.
- Logback, OpenTelemetry SDK and loopback HTTP integration tests check approved output at actual boundaries.

### Not yet completed for a stable release

- No stable version or GitHub Release has been published.
- GitHub Packages upload permissions, fresh authenticated artifact retrieval, and externally hosted production logging/telemetry/network endpoints are not yet validated.
- Remaining GitHub Actions dependency updates and any private security alerts still require maintainer review.
- Final supported compatibility policy and release approval remain pending.

See [release readiness](docs/release-readiness.md) for gates and evidence.
