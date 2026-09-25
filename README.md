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

Use Satellite as a dynamic typed map without Egress.

```java
ParameterMap params = new ParameterMap();

params.put("caseNumber", "C12.12343");
params.put("retry", 3);
params.put("active", true);

String caseNumber = params.getString("caseNumber");
int retry = params.getInt("retry");
boolean active = params.getBoolean("active");
```

**Expected result**

```text
caseNumber = C12.12343
retry      = 3
active     = true
```

---

## 2. Define typed and classified data

A `Key<T>` defines the Java type and static security metadata.

```java
Key<String> userId = Key.string("user.id")
        .classifiedAs(DataClassification.PUBLIC)
        .required();

Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> accessToken = Key.string("auth.access_token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);
```

**Expected definitions**

```text
user.id           -> String / PUBLIC / required
user.email        -> String / CONFIDENTIAL / PERSONAL_DATA
auth.access_token -> String / RESTRICTED / CREDENTIAL
```

Classification answers **how restricted** the value is. Category answers **what kind of data** it is.

---

## 3. Build a SatelliteMap

`SatelliteMap` is an immutable typed data envelope. Origin and trust belong to each runtime value.

```java
String token = "eyJhbGciOi...";

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

**Expected logical content inside the application**

```text
user.id           = user-123
user.email        = user@example.com   [DATABASE / VALIDATED]
auth.access_token = eyJhbGciOi...      [HTTP_HEADER / UNTRUSTED]
```

The values still exist inside the application. Egress policy controls what may leave it.

---

## 4. Apply secure defaults

Evaluate the envelope before sending it to a sink.

```java
EgressProcessor processor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

EgressReport report = processor.process(
        data,
        EgressContext.of(
                EgressSink.LOG,
                "request-diagnostics"));
```

**Expected approved output**

```text
user.id=user-123
user.email=[REDACTED]
```

**Expected decisions**

```text
user.id           -> ALLOW
user.email        -> REDACT
auth.access_token -> DENY
```

The denied token is absent from `report.getOutput()`.

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

Positive rules require both a **sink** and a **purpose**.

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

EgressProcessor networkProcessor =
        new EgressProcessor(policy);

EgressReport allowed = networkProcessor.process(
        data,
        EgressContext.of(
                EgressSink.NETWORK,
                "account-provider"));

EgressReport denied = networkProcessor.process(
        data,
        EgressContext.of(
                EgressSink.NETWORK,
                "analytics"));
```

**Expected result**

```text
purpose=account-provider
user.email -> ALLOW -> user@example.com

purpose=analytics
user.email -> DENY  -> absent from output
```

The same value can be allowed for one legitimate use and denied for another.

---

## 6. Match policies by metadata

Policies can match classification, category, origin, trust, sink and purpose.

```java
PolicyRule redactUntrustedPii = PolicyRule.builder()
        .category(DataCategory.PERSONAL_DATA)
        .origin(DataOrigin.USER_INPUT)
        .trustLevel(TrustLevel.UNTRUSTED)
        .sink(EgressSink.LOG)
        .action(EgressAction.REDACT)
        .reasonCode("UNTRUSTED_PII_IN_LOG")
        .build();

EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(redactUntrustedPii)
        .add(new DefaultEgressRule())
        .build();

SatelliteMap input = SatelliteMap.builder()
        .put(
                email,
                "user@example.com",
                ValueMetadata.of(
                        DataOrigin.USER_INPUT,
                        TrustLevel.UNTRUSTED))
        .build();

EgressReport result = new EgressProcessor(policy).process(
        input,
        EgressContext.of(EgressSink.LOG, "request-log"));
```

**Expected output**

```text
user.email=[REDACTED]
reason=UNTRUSTED_PII_IN_LOG
```

All configured matchers must match before the rule applies.

---

## 7. Pseudonymize instead of exposing

Use `TOKENIZE` when correlation is needed without revealing the original value.

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

byte[] hmacKey = loadSecretFromKms(); // At least 32 bytes.

EgressProcessor processor = new EgressProcessor(
        policy,
        new ConstantRedactor(),
        new HmacSha256Tokenizer(hmacKey));

SatelliteMap analyticsData = SatelliteMap.builder()
        .put(customerId, "45783910")
        .build();

EgressReport result = processor.process(
        analyticsData,
        EgressContext.of(
                EgressSink.STORAGE,
                "analytics"));
```

**Expected output**

```text
customer.id=hmac-sha256:<stable-token>
```

The exact token depends on the HMAC secret. The original value `45783910` is not exported.

For the same key, value and secret, the token is deterministic.

---

## 8. Privacy violations

Denied egress creates a `PrivacyViolation`.

```java
EgressReport report =
        new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(
                        data,
                        EgressContext.of(
                                EgressSink.LOG,
                                "request-diagnostics"));

for (PrivacyViolation violation : report.getViolations()) {
    System.out.println(violation.getReasonCode());
}
```

**Expected output**

```text
SECRET_CATEGORY_DENIED
```

The violation identifies the blocked key and policy context, but never contains the protected token value.

---

## 9. Safe SLF4J logging

The SLF4J adapter runs policy enforcement before writing the log line.

```java
EgressProcessor loggingProcessor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

Slf4jEgressLogger logger = new Slf4jEgressLogger(
        LoggerFactory.getLogger(MyService.class),
        loggingProcessor);

logger.info(
        "Processing authentication request",
        data,
        "authentication-diagnostics");
```

**Expected log**

```text
Processing authentication request {user.email=[REDACTED], user.id=user-123}
```

`auth.access_token` is absent.

Control characters are escaped to reduce log-injection risk.

---

## 10. Safe JSON

Jackson serialization also passes through policy enforcement.

```java
JacksonEgressSerializer serializer =
        new JacksonEgressSerializer(
                new ObjectMapper(),
                new EgressProcessor(
                        EgressPolicyEngine.secureDefaults()));

String json = serializer.toJson(
        data,
        "public-api-response");
```

**Expected JSON**

```json
{
  "user.id": "user-123"
}
```

Why:

```text
user.id           -> PUBLIC       -> included
user.email        -> CONFIDENTIAL -> denied for generic serialization
auth.access_token -> CREDENTIAL   -> denied
```

The serializer only receives the policy-approved representation.

---

## 11. Safe OpenTelemetry attributes

Apply the same policy before values become trace attributes.

```java
OpenTelemetrySpanAdapter adapter =
        new OpenTelemetrySpanAdapter(
                new EgressProcessor(
                        EgressPolicyEngine.secureDefaults()));

adapter.applyToSpan(
        Span.current(),
        data,
        "request-trace");
```

**Expected span attributes**

```text
user.id=user-123
user.email=[REDACTED]
auth.access_token=<absent>
```

Sensitive values cannot be explicitly allowed **raw** into observability sinks. Use redaction, tokenization or denial.

---

## 12. Schema validation

A `SatelliteSchema` defines the allowed typed keys and required fields.

```java
SatelliteSchema userSchema = SatelliteSchema.builder("User")
        .required(userId)
        .optional(email)
        .optional(accessToken)
        .build();

ValidationResult valid = userSchema.validate(data);
```

**Expected result**

```text
valid.isValid() = true
errors          = []
```

Missing a required key fails validation:

```java
SatelliteMap missingUserId = SatelliteMap.builder()
        .put(email, "user@example.com")
        .build();

ValidationResult invalid =
        userSchema.validate(missingUserId);
```

**Expected result**

```text
invalid.isValid() = false
error.code        = REQUIRED_VALUE_MISSING
error.key         = user.id
```

---

## 13. JSON Schema 2020-12

Export the same schema to JSON Schema.

```java
JsonNode jsonSchema =
        new JsonSchemaExporter(
                new ObjectMapper())
                .export(userSchema);
```

**Expected JSON Schema structure**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "User",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "user.id": {
      "type": "string",
      "x-satellite-classification": "PUBLIC",
      "x-satellite-java-type": "java.lang.String"
    },
    "user.email": {
      "type": "string",
      "x-satellite-classification": "CONFIDENTIAL",
      "x-satellite-java-type": "java.lang.String",
      "x-satellite-categories": ["PERSONAL_DATA"]
    }
  },
  "required": ["user.id"]
}
```

The real export also contains the other keys present in `userSchema`.

Import the supported flat-object subset:

```java
SatelliteSchema imported =
        new JsonSchemaImporter().importSchema(jsonSchema);
```

**Expected result**

```text
imported.name            = User
imported.required        = [user.id]
imported.user.email.type = java.lang.String
imported.user.email.classification = CONFIDENTIAL
```

Ambiguous or unsupported definitions are rejected instead of guessed.

---

## 14. Migrate from ParameterMap

Existing applications can keep ParameterMap and add Egress only at the boundary.

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

**Expected result**

```text
safeData[user.id]    = user-123
safeData[user.email] = user@example.com
ignoredKeys          = []
```

Strict mode rejects unknown fields:

```java
params.put("forgotten-secret", "do-not-export");

ParameterMapEgressBridge.toSatelliteMap(
        params,
        Arrays.asList(userId, email),
        ValueMetadata.unknown());
```

**Expected result**

```text
IllegalArgumentException:
Unclassified ParameterMap key: forgotten-secret
```

Lenient migration is explicit:

```java
BridgeResult result = ParameterMapEgressBridge.toSatelliteMapLenient(
        params,
        Arrays.asList(userId, email),
        ValueMetadata.unknown());
```

**Expected result**

```text
ignoredKeys = [forgotten-secret]
```

Convert only policy-approved output back to ParameterMap:

```java
EgressReport safeReport =
        new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(
                        safeData,
                        EgressContext.of(
                                EgressSink.LOG,
                                "migration-log"));

ParameterMap safe =
        ParameterMapEgressBridge.toSafeParameterMap(safeReport);
```

**Expected result**

```text
user.id    = user-123
user.email = [REDACTED]
```

Denied values are never restored.

---

## 15. Hardened PMAP/XML parsing

PMAP/XML parsing blocks DTDs/external entities and applies finite resource limits.

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

String xml =
        "<m><s n=\"status\">ok</s></m>";

ParameterMap parsed = parser.getMap(
        new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));

String status = parsed.getString("status");
```

**Expected result**

```text
status = ok
```

**Expected behavior for invalid input**

```text
XXE / DTD              -> rejected
input over byte limit  -> rejected
Reader over char limit -> rejected
nesting over maxDepth  -> rejected
too many entries       -> rejected
oversized text value   -> rejected
```

Use `maxInputBytes` for `InputStream` and `maxInputCharacters` for `Reader`.

---

## Security guarantees

Satellite is designed around explicit rules:

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

**Expected result**

```text
BUILD SUCCESS
```

The repository includes Java 11/17/21 CI, CodeQL, Dependabot, CycloneDX SBOM generation, module-boundary enforcement and guarded release workflows.

---

## Status

Satellite 2.x is under active development. The API may still change before the first stable 2.x release.

Legacy ParameterMap and legacy logging remain isolated in dedicated modules.

## License

MIT.
