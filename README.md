# Satellite

**Typed dynamic data and policy-enforced application egress for Java.**

Satellite is evolving as two independent libraries in one repository:

1. **ParameterMap** — the existing dynamic typed data container, kept independently usable.
2. **Egress** — a security/privacy enforcement layer that decides what application data may leave through logs, traces, serialization, network or storage sinks.

They can be used separately. An optional bridge connects them when both are useful.

> The 2.x API is currently under development on `satellite-v2-foundation`. It is not a published stable release yet.

## Why Satellite Egress?

Most redaction solutions work after data has already reached a logging or observability pipeline. Satellite Egress is designed to enforce policy **inside the application, before the value reaches the sink**.

A value carries two kinds of metadata:

- static semantics on `Key<T>`: classification and data categories;
- runtime metadata on the value: origin and trust level.

Every egress adapter passes the data through an explicit policy engine. There is intentionally no generic raw-map export method on `SatelliteMap`.

## Modules

| Module | Purpose |
| --- | --- |
| `satellite-parametermap` | Legacy/modern ParameterMap functionality. Independent from Egress. |
| `satellite-legacy-logging` | Existing JSON/SLF4J logger isolated for compatibility. |
| `satellite-egress-core` | `Key<T>`, `SatelliteMap`, classification, categories, origin/trust and schema validation. |
| `satellite-egress-policy` | Fail-closed policy engine, redaction, HMAC tokenization and privacy-violation reports. |
| `satellite-egress-observability` | Policy-enforced SLF4J adapter. |
| `satellite-egress-jackson` | Policy-enforced Jackson serialization and JSON Schema 2020-12 export. |
| `satellite-egress-opentelemetry` | Policy-enforced OpenTelemetry span attributes. |
| `satellite-parametermap-egress-bridge` | Optional strict bridge between ParameterMap and Egress. |

ParameterMap does not depend on Egress. Egress does not depend on ParameterMap. Only the bridge depends on both.

## Egress example

```java
Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> token = Key.string("auth.token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);

SatelliteMap data = SatelliteMap.builder()
        .put(email, "user@example.com",
                ValueMetadata.of(DataOrigin.USER_INPUT, TrustLevel.VALIDATED))
        .put(token, accessToken,
                ValueMetadata.of(DataOrigin.HTTP_HEADER, TrustLevel.UNTRUSTED))
        .build();

EgressProcessor processor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

EgressReport report = processor.process(
        data,
        EgressContext.of(EgressSink.LOG, "request-diagnostics"));
```

With the secure default policy, the email is represented as `[REDACTED]` in observability output and the credential is omitted and reported as a violation.

## Purpose-limited explicit policies

Applications can add narrowly scoped rules before the secure default rule:

```java
EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(PolicyRule.builder()
                .key(email)
                .sink(EgressSink.NETWORK)
                .purpose("account-provider")
                .action(EgressAction.ALLOW)
                .reasonCode("ACCOUNT_EMAIL_REQUIRED")
                .build())
        .add(new DefaultEgressRule())
        .build();
```

That rule does not authorize the same email for a different purpose such as `analytics`.

Rules can also match:

- `DataClassification`
- `DataCategory`
- `DataOrigin`
- `TrustLevel`
- sink
- purpose
- key

## Redaction and pseudonymization

The policy engine supports four decisions:

- `ALLOW`
- `REDACT`
- `TOKENIZE`
- `DENY`

For stable correlation without disclosing the original value, `HmacSha256Tokenizer` provides deterministic pseudonymization. The HMAC key must be supplied by the application and must be at least 32 bytes.

If a policy requests tokenization but no tokenizer is configured, processing fails closed and the value is not emitted.

## Safe adapters

### SLF4J

`Slf4jEgressLogger` accepts a `SatelliteMap`, never a raw map. It applies policy before logging and escapes control characters to reduce log-injection risk.

### Jackson

`JacksonEgressSerializer` processes the envelope with the `SERIALIZATION` sink before producing a `JsonNode` or JSON string.

### OpenTelemetry

`OpenTelemetrySpanAdapter` processes data with the `TRACE` sink before adding attributes to a span.

## Schemas

`SatelliteSchema` validates required and unknown keys.

`JsonSchemaExporter` exports a schema as JSON Schema Draft 2020-12 and preserves Satellite metadata through extension fields such as:

- `x-satellite-classification`
- `x-satellite-categories`
- `x-satellite-java-type`

## ParameterMap

ParameterMap remains usable without any Egress dependency:

```java
ParameterMap parameters = new ParameterMap();
parameters.put("caseNumber", "C12.12343");
parameters.put("step", "Assignment");
```

The PMAP/XML parser is hardened against DTD/external-entity processing and supports configurable resource limits:

```java
PMapParserLimits limits = PMapParserLimits.builder()
        .maxDepth(32)
        .maxEntries(5_000)
        .maxCollectionSize(2_000)
        .maxTextLength(256_000)
        .maxInputBytes(5 * 1024 * 1024L)
        .build();

StreamedPMapParser parser = new StreamedPMapParser(limits);
```

Caller-owned output streams are not closed by PMAP serialization.

## ParameterMap + Egress

The bridge is strict by default. Every ParameterMap field must have an explicit typed `Key<?>` definition or conversion fails.

```java
BridgeResult bridged = ParameterMapEgressBridge.toSatelliteMap(
        parameters,
        keys,
        ValueMetadata.of(DataOrigin.APPLICATION, TrustLevel.VALIDATED));
```

A separately named lenient migration API exists for deliberate staged migrations and reports ignored keys.

## Security properties

The current foundation enforces these invariants:

- no raw `SatelliteMap -> Map<String, Object>` export API;
- default policy is fail-closed;
- credentials/secrets are denied by default;
- restricted data requires explicit policy;
- privacy-sensitive data is redacted for observability by default;
- explicit authorization can be purpose-limited;
- conflicting `Key` definitions with the same external name are rejected;
- denied values are absent from output;
- violation reports do not contain protected values;
- `SatelliteMap` / `SatelliteEntry` string representations do not contain values;
- PMAP/XML DTD and external entities are disabled;
- PMAP parsing has configurable resource budgets;
- CI verifies Java 11, 17 and 21;
- CodeQL security-extended analysis and dependency review are configured;
- `mvn verify` generates an aggregate CycloneDX SBOM;
- release publication refuses `-SNAPSHOT` versions.

See [docs/security-model.md](docs/security-model.md) and [SECURITY.md](SECURITY.md).

## Build

```bash
mvn --batch-mode --no-transfer-progress verify
```

The foundation currently targets Java 11 and is tested on Java 11, 17 and 21.

## Current roadmap

The next hardening/productization steps include parser fuzzing/property-based tests, benchmark suites, JSON Schema import, Maven Central release automation, signed/provenance releases and richer integrations.

## License

MIT
