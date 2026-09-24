# Satellite

**Typed dynamic data with security-aware egress controls for Java.**

Satellite keeps the useful part of dynamic application data while making the security boundary explicit: values have typed keys, classification and privacy categories; outbound sinks are evaluated by policy before data reaches logs, traces, JSON, storage or the network.

> Status: the 2.x API is under active development on `satellite-v2-foundation`. The legacy `ParameterMap` and logging APIs remain isolated in compatibility modules.

## Why Satellite?

Java applications routinely move the same data through maps, JSON, logs, traces and external APIs. A value that is safe in one sink can be a privacy or security incident in another.

Satellite models that decision directly:

```java
Key<String> userId = Key.string("user.id")
        .classifiedAs(DataClassification.PUBLIC);

Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> accessToken = Key.string("auth.access_token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);

SatelliteMap data = SatelliteMap.builder()
        .put(userId, "42")
        .put(email, "user@example.com")
        .put(accessToken, token)
        .build();

EgressReport report = new EgressProcessor(EgressPolicyEngine.secureDefaults())
        .process(data, EgressContext.of(EgressSink.LOG, "request-diagnostics"));
```

With the secure defaults, public data is allowed, personal/confidential data is redacted in observability sinks, and credentials/restricted data are denied. Applications can add explicit rules before the default rule when a specific use case has a legitimate egress requirement.

## Modules

| Module | Purpose |
| --- | --- |
| `satellite-egress-core` | Typed keys, immutable envelopes, schema validation, classification, privacy categories, origin and trust metadata |
| `satellite-egress-policy` | Fail-closed egress decisions, redaction, HMAC tokenization, violation reports |
| `satellite-egress-observability` | Policy-enforced SLF4J adapter with log-control-character escaping |
| `satellite-egress-jackson` | Policy-enforced JSON serialization and JSON Schema 2020-12 export |
| `satellite-egress-opentelemetry` | Policy-enforced OpenTelemetry span attributes |
| `satellite-parametermap-egress-bridge` | Strict migration bridge from legacy `ParameterMap` |
| `satellite-parametermap` | Legacy dynamic map and PMAP/XML compatibility |
| `satellite-legacy-logging` | Legacy JSON logging compatibility |

The egress modules are optional. Using `satellite-parametermap` alone does not pull in the new policy or observability stack.

## Secure defaults

The default policy is intentionally conservative:

- `CREDENTIAL` and `SECRET`: denied.
- `RESTRICTED`: denied unless an explicit application rule is evaluated first.
- `CONFIDENTIAL` and privacy-sensitive categories: redacted for logs/traces/metrics/audit; denied for data sinks by default.
- `INTERNAL`: allowed for local observability/storage, redacted for generic serialization, denied for network egress.
- `PUBLIC`: allowed.

Denied values are not included in `EgressReport.getOutput()`, and violation objects contain metadata and reason codes rather than protected values.

## Explicit policy

```java
EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(EgressRules.forKey(
                email,
                EgressSink.NETWORK,
                EgressAction.ALLOW,
                "CONSENTED_EMAIL_EXPORT"))
        .add(new DefaultEgressRule())
        .build();
```

Rule order matters. Put narrow application rules first and keep `DefaultEgressRule` last as the conservative fallback.

## Pseudonymization

For stable pseudonyms, configure `HmacSha256Tokenizer` with a secret of at least 32 bytes and an explicit `TOKENIZE` rule. Tokens are domain-separated by key name. Do not use a plain hash for low-entropy identifiers.

## Legacy ParameterMap migration

The bridge rejects unclassified source fields by default:

```java
BridgeResult bridged = ParameterMapEgressBridge.toSatelliteMap(
        parameterMap,
        Arrays.asList(userId, email, accessToken),
        ValueMetadata.of(DataOrigin.HTTP_REQUEST, TrustLevel.UNTRUSTED));
```

Use `toSatelliteMapLenient` only as an explicit migration choice; it returns the ignored key names so the omission is visible.

## Safe JSON

```java
JacksonEgressSerializer serializer = new JacksonEgressSerializer(
        new ObjectMapper(),
        new EgressProcessor(policy));

String json = serializer.toJson(data, "public-api-response");
```

Serialization never receives the raw envelope map. It serializes only the policy-approved representation.

## OpenTelemetry

```java
OpenTelemetrySpanAdapter adapter =
        new OpenTelemetrySpanAdapter(new EgressProcessor(policy));

adapter.applyToSpan(Span.current(), data, "request-trace");
```

## JSON Schema 2020-12

```java
SatelliteSchema schema = SatelliteSchema.builder("User")
        .required(userId.required())
        .optional(email)
        .build();

JsonNode jsonSchema = new JsonSchemaExporter(new ObjectMapper()).export(schema);
```

The exporter uses JSON Schema 2020-12 and adds `x-satellite-*` extensions for classification, categories and Java type metadata.

## Hardened PMAP/XML compatibility

Legacy PMAP parsing disables DTDs and external entities and now enforces configurable resource budgets:

```java
PMapParserLimits limits = PMapParserLimits.builder()
        .maxInputBytes(2 * 1024 * 1024)
        .maxDepth(32)
        .maxEntries(5000)
        .maxCollectionSize(2000)
        .maxTextLength(256 * 1024)
        .build();

StreamedPMapParser parser = new StreamedPMapParser(limits);
```

Defaults are intentionally finite. Tune them to the trust boundary and expected payload size.

## Build

```bash
mvn --batch-mode verify
```

CI runs the reactor on Java 11, 17 and 21. The build also generates a CycloneDX SBOM. CodeQL and dependency review workflows are included.

## Compatibility

- Java baseline: 11.
- Legacy APIs are preserved in dedicated modules.
- The 2.x egress API is additive and intentionally separate from the old logging implementation.
- No concrete logging backend is required by the new observability module.

## Security

See [SECURITY.md](SECURITY.md) for vulnerability reporting and the security model.

## License

MIT.
