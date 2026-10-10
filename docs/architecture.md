# Satellite architecture

Satellite 2.x separates flexible application data from security-aware egress enforcement.

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

`SatelliteData` is the dynamic-data API. It provides typed retrieval, conversion helpers, nested data and PMAP/XML interoperability.

`DataDefinition` optionally adds allowed fields, types, required values and defaults.

Satellite Data is intentionally usable without any Egress dependency.

## Egress model

A `Key<T>` owns static semantics:

- external name;
- Java type;
- data classification;
- data categories;
- required/optional schema metadata.

Runtime semantics belong to the value:

- `DataOrigin`;
- `TrustLevel`.

An `EgressEnvelope` combines typed values with those semantics. It is immutable after construction and deliberately exposes no raw `Map<String,Object>` export.

The common metadata form is concise:

```java
EgressEnvelope envelope = EgressEnvelope.builder()
        .put(
                email,
                "user@example.com",
                DataOrigin.DATABASE,
                TrustLevel.VALIDATED)
        .build();
```

`ValueMetadata` remains available when a metadata object is useful to the application.

## Progressive API layers

Satellite exposes two API layers over the same policy engine.

### Common path

The common path favors intention-revealing shortcuts:

```java
EgressReport report =
        EgressProcessor.secureDefaults()
                .process(
                        envelope,
                        EgressSink.LOG,
                        "request-log");

EgressRule rule =
        EgressRules.allow(
                email,
                EgressSink.NETWORK,
                "account-provider",
                "ACCOUNT_EMAIL_REQUIRED");
```

Adapters also expose secure factories:

```java
Slf4jEgressLogger.secure(logger);
JacksonEgressSerializer.secure(objectMapper);
OpenTelemetrySpanAdapter.secure();
```

### Advanced path

The detailed API remains available for policies that match several dimensions or need custom components:

```java
PolicyRule rule =
        PolicyRule.builder()
                .category(DataCategory.PERSONAL_DATA)
                .origin(DataOrigin.USER_INPUT)
                .trustLevel(TrustLevel.UNTRUSTED)
                .sink(EgressSink.LOG)
                .action(EgressAction.REDACT)
                .reasonCode("UNTRUSTED_PII_IN_LOG")
                .build();
```

Custom `Redactor`, `Tokenizer`, violation listeners and explicit `EgressContext` instances all use the same underlying decision pipeline.

The ergonomic layer is additive. It does not introduce a second policy model.

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
EgressProcessor
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

Positive decisions (`ALLOW` and `TOKENIZE`) require both a sink and a purpose. The convenience helpers preserve this requirement:

```java
EgressRules.allow(key, sink, purpose, reasonCode);
EgressRules.tokenize(key, sink, purpose, reasonCode);
```

Restrictive rules can be scoped only to a sink or additionally to a purpose:

```java
EgressRules.redact(key, sink, reasonCode);
EgressRules.deny(key, sink, reasonCode);
```

For custom policies, `withSecureDefaults()` appends Satellite's conservative fallback after explicit rules:

```java
EgressPolicyEngine policy =
        EgressPolicyEngine.builder()
                .add(applicationRule)
                .withSecureDefaults()
                .build();
```

Non-bypassable observability guardrails prevent confidential, restricted, secret, credential and privacy-sensitive data from being emitted raw to logs, traces, metrics or audit sinks.

If redaction or tokenization fails, the processor converts the operation to `DENY`.

## Bridge

`SatelliteDataEgressBridge` is the only primary integration point allowed to know both Satellite Data and Egress.

The shortest strict path reuses an existing `SatelliteSchema`:

```java
DataBridgeResult result =
        SatelliteDataEgressBridge.toEnvelope(
                data,
                schema,
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED);
```

That overload:

- uses the schema as the classified key registry;
- rejects unclassified source fields;
- validates required schema fields;
- preserves typed values and runtime metadata.

The collection-based overload remains available when no schema is needed.

Lenient conversion is explicit and reports ignored field names. It is intended for controlled migrations, not as a permissive default.

## Threat model

The current design directly addresses:

- accidental secret/credential logging;
- accidental PII propagation to observability;
- generic serialization of internal/restricted values;
- unclassified dynamic fields at the egress boundary;
- missing required fields when schema-backed bridge conversion is used;
- classification downgrade through duplicate external names;
- log injection through control characters;
- deterministic pseudonymization without plain hashes;
- XML external entity/DTD processing;
- PMAP parser resource exhaustion.

It does **not** claim full taint tracking, data-flow analysis, consent management, DLP replacement or regulatory compliance certification.

## Optional adapters

Integration modules are separate so applications only pay for what they use.

- SLF4J adapter: policy-enforced logs with control-character escaping.
- Jackson adapter: policy-enforced JSON and JSON Schema 2020-12 interoperability.
- OpenTelemetry adapter: policy-enforced span attributes.

All three adapters accept an explicit `EgressProcessor` for custom policy and also provide a secure-default factory for the common path.

## Release safety

The reactor is tested on Java 11, 17 and 21. CodeQL, CycloneDX SBOM generation, dependency review, architecture/naming checks and guarded release publishing are part of the repository hardening.
