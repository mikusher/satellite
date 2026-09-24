# Satellite

**Typed dynamic data and security-aware application egress for Java.**

Satellite is one repository with independent Maven modules. You can use the legacy-style ParameterMap by itself, use the security-aware Egress framework by itself, or connect both through an optional bridge.

The current development line is `2.0.0-SNAPSHOT` and keeps Java 11 compatibility while CI verifies Java 11, 17 and 21.

## Modules

- `satellite-parametermap` — typed dynamic map, conversion helpers, PMAP/XML serialization and legacy schema-like constraints.
- `satellite-legacy-logging` — the original structured logging API, isolated from the new security model.
- `satellite-egress-core` — `Key<T>`, immutable `SatelliteMap`, classifications, categories, origin/trust metadata and schema validation.
- `satellite-egress-policy` — fail-closed policy engine with `ALLOW`, `REDACT`, `TOKENIZE` and `DENY`.
- `satellite-egress-observability` — policy-enforced SLF4J adapter.
- `satellite-egress-jackson` — policy-enforced Jackson serialization and JSON Schema 2020-12 export.
- `satellite-egress-opentelemetry` — policy-enforced OpenTelemetry span attributes.
- `satellite-parametermap-egress-bridge` — optional strict bridge between ParameterMap and the Egress model.

ParameterMap does not depend on Egress. Egress does not depend on ParameterMap. Only the bridge knows both.

## ParameterMap

```java
ParameterMap parameters = new ParameterMap();
parameters.put("caseNumber", "C12.12343");
parameters.put("step", "Assignment");

String caseNumber = parameters.getString("caseNumber");
```

The PMAP/XML parser disables DTD/external entities and supports configurable limits for input size, nesting depth, entry count, collection size and text length.

```java
StreamedPMapParser parser = new StreamedPMapParser(
        PMapParserLimits.builder()
                .maxDepth(32)
                .maxEntries(5_000)
                .maxInputBytes(2 * 1024 * 1024L)
                .build());
```

## Security-aware Egress

The Egress side is designed around one rule: **data should be classified before it leaves the application**.

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
        .put(userId, "user-123")
        .put(email, "user@example.com",
                ValueMetadata.of(DataOrigin.DATABASE, TrustLevel.VALIDATED))
        .put(accessToken, token,
                ValueMetadata.of(DataOrigin.HTTP_HEADER, TrustLevel.UNTRUSTED))
        .build();
```

The default policy is conservative:

- credentials and secrets are denied;
- `RESTRICTED` values are denied;
- confidential/privacy-sensitive values are redacted in observability sinks;
- internal values require explicit policy for network egress;
- unmatched policy evaluation fails closed.

```java
EgressProcessor processor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

EgressReport report = processor.process(
        data,
        EgressContext.of(EgressSink.LOG, "request-log"));
```

The report contains only the policy-approved representation plus machine-readable decisions and privacy violations. Violation objects deliberately do not contain the protected value.

## Explicit egress rules

Applications can add narrow allow/redact/tokenize/deny rules before the secure default rule.

```java
EgressPolicyEngine policies = EgressPolicyEngine.builder()
        .add(EgressRules.forKey(
                email,
                EgressSink.NETWORK,
                EgressAction.ALLOW,
                "CONSENTED_EMAIL_EXPORT"))
        .add(new DefaultEgressRule())
        .build();
```

For correlation without disclosure, `HmacSha256Tokenizer` provides deterministic pseudonymization with key-name domain separation.

## Safe adapters

### SLF4J

```java
Slf4jEgressLogger logger = new Slf4jEgressLogger(
        LoggerFactory.getLogger(MyService.class),
        processor);

logger.info("Processing request", data, "request-log");
```

The adapter processes the envelope before logging, omits denied fields, redacts sensitive values and escapes control characters.

### Jackson

```java
JacksonEgressSerializer serializer =
        new JacksonEgressSerializer(new ObjectMapper(), processor);

String json = serializer.toJson(data, "api-response");
```

### OpenTelemetry

```java
OpenTelemetrySpanAdapter adapter =
        new OpenTelemetrySpanAdapter(processor);

adapter.applyToSpan(Span.current(), data, "request-trace");
```

Adapters accept `SatelliteMap`; they do not expose a generic raw-map bypass.

## Schema

```java
SatelliteSchema schema = SatelliteSchema.builder("User")
        .required(userId)
        .optional(email)
        .build();

ValidationResult validation = schema.validate(data);
```

The Jackson module can export the schema as JSON Schema 2020-12 while retaining Satellite metadata through `x-satellite-*` extension properties.

## ParameterMap bridge

Migration is explicit. By default, a ParameterMap field without a registered typed `Key<?>` makes conversion fail.

```java
BridgeResult bridged = ParameterMapEgressBridge.toSatelliteMap(
        parameters,
        Arrays.asList(userId, email),
        ValueMetadata.of(DataOrigin.APPLICATION, TrustLevel.VALIDATED));
```

A lenient mode exists only as an explicit migration opt-in and reports ignored keys.

## Security posture

The current foundation includes:

- XML XXE/DTD protection;
- bounded PMAP parsing;
- fail-closed egress policies;
- classification collision/downgrade protection;
- deterministic HMAC-SHA256 pseudonymization;
- privacy violation reporting without protected values;
- policy-enforced SLF4J, Jackson and OpenTelemetry adapters;
- CI on Java 11/17/21;
- Dependabot;
- CodeQL;
- pull-request dependency review;
- CycloneDX SBOM generation;
- release workflow that refuses snapshot publication.

See [SECURITY.md](SECURITY.md) and [docs/architecture.md](docs/architecture.md).

## Build

```bash
mvn --batch-mode --no-transfer-progress verify
```

## Status

The 2.x API is under active development. It is not yet a stable release and breaking changes are still possible before `2.0.0`.

## License

MIT
