# Satellite

**Typed dynamic data and policy-controlled data egress for Java.**

Satellite has two independent sides:

- **Satellite Data** — dynamic typed application data.
- **Satellite Egress** — classify data and control what may leave the application.

Use either side alone, or connect them with the optional bridge.

> Current line: `2.0.0-SNAPSHOT` · Java 11 baseline · tested on Java 11, 17 and 21.

## Modules

| Module | Purpose |
| --- | --- |
| `satellite-data` | `SatelliteData`, `DataDefinition`, conversions and PMAP/XML |
| `satellite-egress-core` | `Key<T>`, `EgressEnvelope`, metadata and schemas |
| `satellite-egress-policy` | `ALLOW`, `REDACT`, `TOKENIZE`, `DENY` |
| `satellite-egress-observability` | Policy-enforced SLF4J |
| `satellite-egress-jackson` | Policy-enforced JSON + JSON Schema 2020-12 |
| `satellite-egress-opentelemetry` | Policy-enforced OpenTelemetry attributes |
| `satellite-data-egress-bridge` | Optional Satellite Data → Egress bridge |
| `satellite-legacy-logging` | Legacy logging compatibility |

**Boundary rule:** Satellite Data never depends on Egress. Egress never depends on Satellite Data. Only the bridge knows both.

---

## 1. SatelliteData

Use `SatelliteData` when you need flexible application data with typed getters.

```java
SatelliteData data = new SatelliteData();

data.put("caseNumber", "C12.12343");
data.put("retry", 3);
data.put("active", true);

String caseNumber = data.getString("caseNumber");
int retry = data.getInt("retry");
boolean active = data.getBoolean("active");
```

**Expected result**

```text
caseNumber = C12.12343
retry      = 3
active     = true
```

### Optional DataDefinition

Use `DataDefinition` when fields need constraints, required values or defaults.

```java
DataDefinition definition =
        new DataDefinition("request", "Request data");

definition.addString("id", "Request identifier");

SatelliteData request = new SatelliteData(definition);
request.put("id", "REQ-123");

String id = request.getString("id");
```

**Expected result**

```text
id = REQ-123
```

---

## 2. Define classified keys

`Key<T>` defines the Java type and static security metadata used by Egress.

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

Classification answers **how restricted** data is. Category answers **what kind of data** it is.

---

## 3. Build an EgressEnvelope

`EgressEnvelope` is the immutable security-aware representation evaluated before data leaves the application.

```java
String token = "eyJhbGciOi...";

EgressEnvelope envelope = EgressEnvelope.builder()
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

**Expected logical content**

```text
user.id           = user-123
user.email        = user@example.com   [DATABASE / VALIDATED]
auth.access_token = eyJhbGciOi...      [HTTP_HEADER / UNTRUSTED]
```

No value is redacted at this stage. The envelope holds classified data **inside** the application. Policy decides what may leave it.

---

## 4. Apply secure defaults

```java
EgressProcessor processor =
        new EgressProcessor(EgressPolicyEngine.secureDefaults());

EgressReport report = processor.process(
        envelope,
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

| Data | Default behavior |
| --- | --- |
| `PUBLIC` | Allow |
| `INTERNAL` | Allow locally; restrict external egress |
| `CONFIDENTIAL` | Redact in observability; deny data sinks by default |
| `RESTRICTED` | Deny |
| `CREDENTIAL` / `SECRET` | Deny |

No matching policy means **DENY**.

---

## 5. Allow one specific use

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
        envelope,
        EgressContext.of(
                EgressSink.NETWORK,
                "account-provider"));

EgressReport denied = networkProcessor.process(
        envelope,
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

---

## 6. Match policy by metadata

Rules can match classification, category, origin, trust, sink and purpose.

```java
PolicyRule redactUntrustedPii = PolicyRule.builder()
        .category(DataCategory.PERSONAL_DATA)
        .origin(DataOrigin.USER_INPUT)
        .trustLevel(TrustLevel.UNTRUSTED)
        .sink(EgressSink.LOG)
        .action(EgressAction.REDACT)
        .reasonCode("UNTRUSTED_PII_IN_LOG")
        .build();

EgressEnvelope input = EgressEnvelope.builder()
        .put(
                email,
                "user@example.com",
                ValueMetadata.of(
                        DataOrigin.USER_INPUT,
                        TrustLevel.UNTRUSTED))
        .build();

EgressPolicyEngine policy = EgressPolicyEngine.builder()
        .add(redactUntrustedPii)
        .add(new DefaultEgressRule())
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

---

## 7. Pseudonymize instead of exposing

Use `TOKENIZE` when correlation is needed without revealing the original identifier.

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

EgressEnvelope analytics = EgressEnvelope.builder()
        .put(customerId, "45783910")
        .build();

EgressReport result = processor.process(
        analytics,
        EgressContext.of(
                EgressSink.STORAGE,
                "analytics"));
```

**Expected output**

```text
customer.id=hmac-sha256:<stable-token>
```

The exact token depends on the secret. `45783910` is not exported.

---

## 8. Privacy violations

Denied egress creates metadata-only `PrivacyViolation` records.

```java
EgressReport report =
        new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(
                        envelope,
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

Violations contain policy context, never the protected value.

---

## 9. Safe adapters

### SLF4J

```java
Slf4jEgressLogger logger = new Slf4jEgressLogger(
        LoggerFactory.getLogger(MyService.class),
        new EgressProcessor(EgressPolicyEngine.secureDefaults()));

logger.info(
        "Processing authentication request",
        envelope,
        "authentication-diagnostics");
```

**Expected log**

```text
Processing authentication request {user.email=[REDACTED], user.id=user-123}
```

The access token is absent. Control characters are escaped.

### Jackson

```java
JacksonEgressSerializer serializer =
        new JacksonEgressSerializer(
                new ObjectMapper(),
                new EgressProcessor(
                        EgressPolicyEngine.secureDefaults()));

String json = serializer.toJson(
        envelope,
        "public-api-response");
```

**Expected JSON**

```json
{
  "user.id": "user-123"
}
```

`user.email` is denied for generic serialization and the credential is denied.

### OpenTelemetry

```java
OpenTelemetrySpanAdapter adapter =
        new OpenTelemetrySpanAdapter(
                new EgressProcessor(
                        EgressPolicyEngine.secureDefaults()));

adapter.applyToSpan(
        Span.current(),
        envelope,
        "request-trace");
```

**Expected span attributes**

```text
user.id=user-123
user.email=[REDACTED]
auth.access_token=<absent>
```

Sensitive values cannot be allowed **raw** into observability sinks.

---

## 10. Egress schema validation

```java
SatelliteSchema userSchema = SatelliteSchema.builder("User")
        .required(userId)
        .optional(email)
        .optional(accessToken)
        .build();

ValidationResult valid =
        userSchema.validate(envelope);
```

**Expected result**

```text
valid.isValid() = true
errors          = []
```

Missing the required key:

```java
EgressEnvelope missingUserId = EgressEnvelope.builder()
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

## 11. JSON Schema 2020-12

```java
JsonNode jsonSchema =
        new JsonSchemaExporter(
                new ObjectMapper())
                .export(userSchema);
```

**Expected structure**

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

Import the supported flat-object subset:

```java
SatelliteSchema imported =
        new JsonSchemaImporter().importSchema(jsonSchema);
```

**Expected result**

```text
imported.name                      = User
imported.required                  = [user.id]
imported.user.email.type           = java.lang.String
imported.user.email.classification = CONFIDENTIAL
```

Unsupported or ambiguous definitions are rejected instead of guessed.

---

## 12. Bridge SatelliteData to Egress

Existing dynamic data can enter Egress only through explicitly registered keys.

```java
SatelliteData data = new SatelliteData();
data.put("user.id", "user-123");
data.put("user.email", "user@example.com");

DataBridgeResult bridged =
        SatelliteDataEgressBridge.toEnvelope(
                data,
                Arrays.asList(userId, email),
                ValueMetadata.of(
                        DataOrigin.APPLICATION,
                        TrustLevel.VALIDATED));

EgressEnvelope safeEnvelope =
        bridged.getEnvelope();
```

**Expected result**

```text
safeEnvelope[user.id]    = user-123
safeEnvelope[user.email] = user@example.com
ignoredKeys              = []
```

Strict mode rejects unclassified fields:

```java
data.put("forgotten-secret", "do-not-export");

SatelliteDataEgressBridge.toEnvelope(
        data,
        Arrays.asList(userId, email),
        ValueMetadata.unknown());
```

**Expected result**

```text
IllegalArgumentException:
Unclassified SatelliteData key: forgotten-secret
```

Lenient migration must be explicit:

```java
DataBridgeResult result =
        SatelliteDataEgressBridge.toEnvelopeLenient(
                data,
                Arrays.asList(userId, email),
                ValueMetadata.unknown());
```

**Expected result**

```text
ignoredKeys = [forgotten-secret]
```

Convert only policy-approved output back:

```java
EgressReport safeReport =
        new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(
                        safeEnvelope,
                        EgressContext.of(
                                EgressSink.LOG,
                                "migration-log"));

SatelliteData safe =
        SatelliteDataEgressBridge.toSafeData(safeReport);
```

**Expected result**

```text
user.id    = user-123
user.email = [REDACTED]
```

Denied values are never restored.

---

## 13. Hardened PMAP/XML parsing

The PMAP parser exposes `SatelliteData` directly.

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

SatelliteData parsed = parser.getData(
        new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));

String status = parsed.getString("status");
```

**Expected result**

```text
status = ok
```

**Invalid input behavior**

```text
XXE / DTD              -> rejected
input over byte limit  -> rejected
Reader over char limit -> rejected
nesting over maxDepth  -> rejected
too many entries       -> rejected
oversized text value   -> rejected
```

---

## Compatibility names

Satellite 2.x keeps the previous names temporarily so existing code can migrate gradually.

| Deprecated name | New name |
| --- | --- |
| `ParameterMap` | `SatelliteData` |
| `ParameterInfoMap` | `DataDefinition` |
| `SatelliteMap` | `EgressEnvelope` |
| `satellite-parametermap` | `satellite-data` |
| `satellite-parametermap-egress-bridge` | `satellite-data-egress-bridge` |

Compatibility artifacts depend on the new artifacts. New code should use the new names.

---

## Security guarantees

- outbound data is policy-evaluated before supported sinks;
- unmatched egress fails closed;
- credentials and secrets are denied by default;
- positive rules require a specific sink and purpose;
- sensitive values cannot be emitted raw to observability sinks;
- redaction/tokenization failures become `DENY`;
- violation reports never include protected values;
- duplicate key names cannot silently downgrade classification;
- the Satellite Data bridge rejects unclassified fields by default;
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

## Status

Satellite 2.x is under active development. APIs may still change before the first stable 2.x release.

## License

MIT.
