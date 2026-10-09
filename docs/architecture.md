# Satellite architecture

Satellite 2.x separates dynamic application data from security-aware egress enforcement.

## Dependency direction

```text
satellite-data                         satellite-egress-core
      |                                        |
      |                                satellite-egress-policy
      |                                  |        |        |
      |                                  v        v        v
      |                                slf4j    jackson   opentelemetry
      |
      +------> satellite-data-egress-bridge <----+
```

The build contains an architecture check that fails if Satellite Data starts depending on Egress or Egress starts depending on Satellite Data.

## Satellite Data

`SatelliteData` is the primary dynamic-data API. It provides typed retrieval, conversion helpers, nested data and PMAP/XML interoperability.

`DataDefinition` optionally adds allowed fields, types, required values and defaults.


## Egress model

A `Key<T>` owns static semantics:

- external name;
- Java type;
- data classification;
- data categories;
- required/optional schema metadata.

`ValueMetadata` owns runtime semantics:

- origin;
- trust level.

`EgressEnvelope` combines typed values with this metadata. It is immutable after construction and deliberately exposes no raw `Map<String,Object>` export.


## Egress boundary

Every supported sink follows the same sequence:

```text
EgressEnvelope
      |
      v
EgressPolicyEngine
      |
      v
ALLOW / REDACT / TOKENIZE / DENY
      |
      v
EgressReport
      |
      v
approved sink adapter
```

Denied values never enter `EgressReport.getOutput()`.

## Policy model

Rules are ordered and all configured matchers must match.

Positive decisions (`ALLOW` and `TOKENIZE`) require both a sink and a purpose. This prevents accidental global allow rules.

Non-bypassable observability guardrails prevent confidential, restricted, secret, credential and privacy-sensitive data from being emitted raw to logs, traces, metrics or audit sinks.

If redaction or tokenization fails, the processor converts the operation to `DENY`.

## Bridge

`SatelliteDataEgressBridge` is the only primary integration point allowed to know both Satellite Data and Egress.

Strict conversion rejects every `SatelliteData` field without an explicit `Key<?>`.

Lenient conversion is an explicit migration option and reports ignored field names.

## Threat model

The current design directly addresses:

- accidental secret/credential logging;
- accidental PII propagation to observability;
- generic serialization of internal/restricted values;
- unclassified dynamic fields at the egress boundary;
- classification downgrade through duplicate external names;
- log injection through control characters;
- deterministic pseudonymization without plain hashes;
- XML external entity/DTD processing;
- PMAP parser resource exhaustion.

It does **not** claim full taint tracking, data-flow analysis, consent management, DLP replacement or regulatory compliance certification.

## Optional adapters

Integration modules are separate so applications only pay for what they use.

- SLF4J adapter: API dependency only, no forced logging backend.
- Jackson adapter: policy-enforced JSON and JSON Schema 2020-12 interoperability.
- OpenTelemetry adapter: policy-enforced span attributes.

## Release safety

The reactor is tested on Java 11, 17 and 21. CodeQL, CycloneDX SBOM generation, dependency checks and guarded release publishing are part of the repository hardening.
