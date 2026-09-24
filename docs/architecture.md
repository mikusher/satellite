# Satellite architecture

Satellite 2.x separates the legacy compatibility surface from a new security-aware egress model.

## Dependency direction

```text
satellite-egress-core
        |
        v
satellite-egress-policy
   |        |        |
   v        v        v
slf4j    jackson   opentelemetry

satellite-parametermap ---> satellite-parametermap-egress-bridge
                                   |
                                   +----> egress-core + egress-policy

satellite-legacy-logging ---> satellite-parametermap
```

The core has no dependency on SLF4J, Jackson, OpenTelemetry, Spring or a logging backend.

## Data model

A `Key<T>` owns static semantics: external name, Java type, classification and data categories. A `SatelliteEntry<T>` pairs the key/value with runtime metadata such as origin and trust level. `SatelliteMap` is immutable after construction and deliberately exposes no raw `Map<String,Object>` export.

Duplicate external key names with conflicting definitions are rejected. This prevents a second key definition from downgrading the classification of a protected field before egress.

## Egress boundary

Every supported sink follows the same sequence:

1. application creates or bridges a `SatelliteMap`;
2. `EgressPolicyEngine` evaluates each entry against an `EgressContext`;
3. `EgressProcessor` applies `ALLOW`, `REDACT`, `TOKENIZE` or `DENY`;
4. the sink adapter receives only `EgressReport.getOutput()`;
5. denied operations are represented as metadata-only `PrivacyViolation` records.

The engine denies when no rule returns a decision. `DefaultEgressRule` provides conservative baseline behavior, while explicit application rules can be placed before it.

## Threat model

The current design directly addresses:

- accidental secret/credential logging;
- accidental PII propagation to observability;
- generic serialization of internal/restricted values;
- unclassified fields during legacy `ParameterMap` migration;
- classification downgrade via duplicate external key names;
- log injection via control characters in the SLF4J adapter;
- deterministic pseudonymization without unsalted hashes;
- XML external entity/DTD processing;
- PMAP parser resource exhaustion through bounded depth, input, entries, collections and text.

It does **not** claim full taint tracking, data-flow analysis, consent management, DLP replacement or regulatory compliance certification.

## Policy design

Rules are ordered. A narrow explicit rule may authorize a specific key/sink use case; the default rule should remain last. This keeps exceptions reviewable and prevents a global permissive switch.

A `purpose` string is mandatory in `EgressContext`. It is descriptive context today and is intentionally available for future purpose-aware policies without changing sink APIs.

## Serialization and observability

Adapters are separate Maven modules so applications pay only for integrations they use. The new SLF4J adapter depends on the API only, not Logback or Log4j. Jackson and OpenTelemetry are likewise optional.

## Legacy compatibility

`satellite-parametermap` remains the compatibility module for the original dynamic map and PMAP format. `satellite-legacy-logging` isolates the old logger and its historical dependencies. The strict bridge is the intended migration path into the egress model.

## Release safety

The reactor is tested on Java 11, 17 and 21. Publishing is release/manual only and refuses `-SNAPSHOT` versions. CodeQL, dependency review and CycloneDX SBOM generation are part of the repository hardening.
