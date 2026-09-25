# Satellite

**Typed dynamic data and policy-controlled data egress for Java.**

Satellite provides two independent capabilities:

- **ParameterMap** — dynamic typed data, conversion helpers and PMAP/XML support.
- **Egress** — classify data and control what may leave the application through logs, traces, JSON, storage or the network.

Use either one independently, or connect them with the optional bridge.

> Current line: `2.0.0-SNAPSHOT` · Java 11 baseline · tested on Java 11, 17 and 21.

## Modules

| Module | Use it for |
| --- | --- |
| `satellite-parametermap` | Dynamic typed data and PMAP/XML |
| `satellite-egress-core` | Typed keys, metadata, immutable envelopes and schemas |
| `satellite-egress-policy` | `ALLOW`, `REDACT`, `TOKENIZE`, `DENY` |
| `satellite-egress-observability` | Safe SLF4J output |
| `satellite-egress-jackson` | Safe JSON + JSON Schema 2020-12 |
| `satellite-egress-opentelemetry` | Safe OpenTelemetry attributes |
| `satellite-parametermap-egress-bridge` | Optional ParameterMap → Egress migration |
| `satellite-legacy-logging` | Legacy Satellite logging compatibility |

**Boundary rule:** ParameterMap does not depend on Egress. Egress does not depend on ParameterMap. Only the bridge knows both.

---

## 1. ParameterMap only

Use Satellite as a dynamic typed map without the Egress framework.

```java
ParameterMap params = new ParameterMap();

params.put("caseNumber", "C12.12343");
params.put("retry", 3);
params.put("active", true);

String caseNumber = params.getString("caseNumber");
int retry = params.getInt("retry");
boolean active = params.getBoolean("active");
```

This module remains independently usable.

---

## 2. Define typed and classified data

A `Key<T>` defines the Java type and static security metadata.

```java
Key<String> userId = Key.string("user.id")
        .classifiedAs(DataClassification.PUBLIC);

Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> accessToken = Key.string("auth.access_token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);
```

Classification answers **how restricted** the value is.

```text
PUBLIC
INTERNAL
CONFIDENTIAL
RESTRICTED
```

Category answers **what kind of data** it is.

Examples:

```text
PERSONAL_DATA
CREDENTIAL
SECRET
FINANCIAL
HEALTH
LOCATION
DEVICE_IDENTIFIER
NETWORK_IDENTIFIER
```

---

## 3. Build a SatelliteMap

`SatelliteMap` is an immutable typed data envelope.

Runtime metadata such as origin and trust belongs to the value, not the key.

```java
SatelliteMap data = SatelliteMap.builder()
        .put(userId, "user-123")
        .put(
                email,
                "user@example.com",
                ValueMetadata.of(
                        DataOrigin.DATABASE,
                        TrustLevel.VALIDATED))
        .put(
                accessToken,
                token,
                ValueMetadata.of(
                        DataOrigin.HTTP_HEADER,
                        TrustLevel.UNTRUSTED))
        .build();
```

Supported origins include application data, user input, HTTP headers/body, query parameters, cookies, databases, configuration, environment and third parties.

---

## 4. Apply secure defaults

All outbound data can be evaluated before it reaches a sink.

```java
EgressProcessor processor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

EgressReport report = processor.process(
        data,
        EgressContext.of(
                EgressSink.LOG,
                "request-diagnostics"));
```

For the example above, the approved output is conceptually:

```text
user.id=user-123
user.email=[REDACTED]
```

`auth.access_token` is denied and is not included in the output.

### Default behavior

| Data | Default behavior |
| --- | --- |
| `PUBLIC` | Allow |
| `INTERNAL` | Allow locally; restrict external egress |
| `CONFIDENTIAL` | Redact in observability; deny other sinks by default |
| `RESTRICTED` | Deny |
| `CREDENTIAL` / `SECRET` | Deny |
| Privacy-sensitive categories | Redact in observability; deny other sinks by default |

If no rule matches, the engine **fails closed**.

---

## 5. Allow data for one specific purpose

A positive rule must be scoped to both **sink** and **purpose**.

Example: the email may be sent to an account provider, but not to arbitrary network destinations.

```java
EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(
                PolicyRule.builder()
                        .key(email)
                        .sink(EgressSink.NETWORK)
                        .purpose("account-provider")
                        .action(EgressAction.ALLOW)
                        .reasonCode("ACCOUNT_EMAIL_REQUIRED")
                        .build())
        .add(new DefaultEgressRule())
        .build();
```

This matches:

```java
EgressContext.of(EgressSink.NETWORK, "account-provider");
```

This does not:

```java
EgressContext.of(EgressSink.NETWORK, "analytics");
```

Narrow rules should come before `DefaultEgressRule`.

---

## 6. Match policies by metadata

Policies can match more than a single key.

```java
PolicyRule rule = PolicyRule.builder()
        .category(DataCategory.PERSONAL_DATA)
        .origin(DataOrigin.USER_INPUT)
        .trustLevel(TrustLevel.UNTRUSTED)
        .sink(EgressSink.LOG)
        .action(EgressAction.REDACT)
        .reasonCode("UNTRUSTED_PII_IN_LOG")
        .build();
```

Available matchers include:

- key or key name
- classification
- category
- origin
- trust level
- sink
- purpose

---

## 7. Pseudonymize instead of exposing

Use `TOKENIZE` when correlation is needed without exposing the original value.

```java
Key<String> customerId = Key.string("customer.id")
        .classifiedAs(DataClassification.CONFIDENTIAL);

EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(
                PolicyRule.builder()
                        .key(customerId)
                        .sink(EgressSink.STORAGE)
                        .purpose("analytics")
                        .action(EgressAction.TOKENIZE)
                        .reasonCode("ANALYTICS_PSEUDONYM")
                        .build())
        .add(new DefaultEgressRule())
        .build();

HmacSha256Tokenizer tokenizer =
        new HmacSha256Tokenizer(loadSecretFromKms());

EgressProcessor processor =
        new EgressProcessor(
                policy,
                new ConstantRedactor(),
                tokenizer);
```

The output is deterministic for the same key/value and does not expose the original identifier.

Use a real secret manager for the HMAC key. Do not hard-code it.

---

## 8. Privacy violations

Denied egress creates a `PrivacyViolation`.

```java
EgressReport report = processor.process(
        data,
        EgressContext.of(EgressSink.LOG, "request-diagnostics"));

if (report.hasViolations()) {
    for (PrivacyViolation violation : report.getViolations()) {
        System.out.println(violation.getReasonCode());
    }
}
```

Violations contain metadata such as key name, sink, purpose, classification, category, origin and trust level.

They **do not contain the protected value**.

---

## 9. Safe SLF4J logging

The SLF4J adapter accepts `SatelliteMap`, runs the policy first and only logs the approved representation.

```java
Slf4jEgressLogger logger = new Slf4jEgressLogger(
        LoggerFactory.getLogger(MyService.class),
        processor);

logger.info(
        "Processing authentication request",
        data,
        "authentication-diagnostics");
```

Example result:

```text
Processing authentication request {user.email=[REDACTED], user.id=user-123}
```

Denied credentials are absent.

Control characters are escaped to reduce log-injection risk.

---

## 10. Safe JSON

Jackson serialization also passes through policy enforcement.

```java
JacksonEgressSerializer serializer =
        new JacksonEgressSerializer(
                new ObjectMapper(),
                processor);

String json = serializer.toJson(
        data,
        "public-api-response");
```

The serializer does not receive a raw map bypass. It serializes only `EgressReport.getOutput()`.

---

## 11. Safe OpenTelemetry attributes

Use the same policy model before data becomes trace attributes.

```java
OpenTelemetrySpanAdapter adapter =
        new OpenTelemetrySpanAdapter(processor);

adapter.applyToSpan(
        Span.current(),
        data,
        "request-trace");
```

Example:

```text
http.route=/users/{id}
user.email=[REDACTED]
auth.access_token=<absent>
```

Sensitive values cannot be explicitly allowed **raw** into observability sinks. Use redaction, tokenization or denial.

---

## 12. Schema validation

A `SatelliteSchema` validates which typed keys are expected.

```java
SatelliteSchema userSchema = SatelliteSchema.builder("User")
        .required(userId.required())
        .optional(email)
        .build();

ValidationResult result = userSchema.validate(data);

if (!result.isValid()) {
    result.getErrors().forEach(System.out::println);
}
```

Conflicting definitions using the same external key name are rejected.

---

## 13. JSON Schema 2020-12

Export a Satellite schema:

```java
JsonSchemaExporter exporter =
        new JsonSchemaExporter(new ObjectMapper());

JsonNode jsonSchema = exporter.export(userSchema);
```

Satellite metadata is preserved with `x-satellite-*` extensions.

Example:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "user.email": {
      "type": "string",
      "x-satellite-classification": "CONFIDENTIAL",
      "x-satellite-categories": ["PERSONAL_DATA"],
      "x-satellite-java-type": "java.lang.String"
    }
  }
}
```

Import the supported flat-object subset:

```java
SatelliteSchema imported =
        new JsonSchemaImporter().importSchema(jsonSchema);
```

Ambiguous or unsupported definitions are rejected instead of guessed.

---

## 14. Migrate from ParameterMap

The bridge lets existing applications adopt Egress without replacing ParameterMap.

```java
ParameterMap params = new ParameterMap();
params.put("user.id", "user-123");
params.put("user.email", "user@example.com");

BridgeResult bridged = ParameterMapEgressBridge.toSatelliteMap(
        params,
        Arrays.asList(userId, email),
        ValueMetadata.of(
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED));

SatelliteMap safeData = bridged.getSatelliteMap();
```

Strict mode rejects any ParameterMap field without an explicit `Key<?>`.

```text
Unclassified ParameterMap key: forgotten-secret
```

For controlled migrations, lenient mode is explicit:

```java
BridgeResult result = ParameterMapEgressBridge.toSatelliteMapLenient(
        params,
        Arrays.asList(userId, email),
        ValueMetadata.unknown());

List<String> ignored = result.getIgnoredKeys();
```

Convert policy-approved data back to ParameterMap:

```java
ParameterMap safe =
        ParameterMapEgressBridge.toSafeParameterMap(report);
```

Denied values are not restored.

---

## 15. Hardened PMAP/XML parsing

PMAP/XML parsing disables DTDs and external entities and applies finite resource limits.

```java
PMapParserLimits limits = PMapParserLimits.builder()
        .maxInputBytes(2 * 1024 * 1024L)
        .maxInputCharacters(2 * 1024 * 1024L)
        .maxDepth(32)
        .maxEntries(5_000)
        .maxCollectionSize(2_000)
        .maxTextLength(256 * 1024)
        .build();

StreamedPMapParser parser =
        new StreamedPMapParser(limits);
```

Use `maxInputBytes` for `InputStream` input and `maxInputCharacters` for `Reader` input.

---

## Security guarantees

Satellite is designed around a few explicit rules:

- outbound data is policy-evaluated before supported sinks;
- unmatched egress fails closed;
- credentials and secrets are denied by default;
- positive rules require a specific sink and purpose;
- sensitive values cannot be emitted raw to observability sinks;
- redaction/tokenization failures become `DENY`;
- violation reports never include the protected value;
- duplicate key names cannot silently downgrade classification;
- the ParameterMap bridge rejects unclassified fields by default;
- PMAP/XML parsing blocks XXE/DTD and enforces resource budgets.

See [SECURITY.md](SECURITY.md) and [docs/architecture.md](docs/architecture.md).

---

## Build

```bash
mvn --batch-mode --no-transfer-progress verify
```

The repository includes:

- Java 11 / 17 / 21 CI
- CodeQL
- Dependabot
- dependency review when GitHub Dependency Graph is available
- CycloneDX SBOM generation
- module-boundary enforcement
- release workflow with snapshot publication protection

---

## Status

Satellite 2.x is under active development. The API may still change before the first stable 2.x release.

The legacy ParameterMap and legacy logging code remain isolated in dedicated modules.

## License

MIT.
