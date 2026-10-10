# Satellite

**Typed dynamic data and policy-controlled egress for Java.**

Satellite helps Java applications work with flexible data and decide **what may leave the application** before it reaches logs, traces, JSON, network calls or storage.

Use the parts you need:

- **Satellite Data** — dynamic data with typed access and optional definitions.
- **Satellite Egress** — classify values and enforce `ALLOW`, `REDACT`, `TOKENIZE` or `DENY`.
- **Bridge** — optionally move `SatelliteData` into the Egress model.

> Satellite 2.x · Java 11 baseline · tested on Java 11, 17 and 21.

## Why?

Sensitive data often leaks at application boundaries.

Satellite makes that boundary explicit:

```text
application data
      |
      v
EgressEnvelope
      |
      v
policy: value + sink + purpose + origin + trust
      |
      v
ALLOW / REDACT / TOKENIZE / DENY
      |
      v
approved output
```

## Quick start

Define what each value means:

```java
Key<String> userId = Key.string("user.id")
        .classifiedAs(DataClassification.PUBLIC);

Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> token = Key.string("auth.token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);
```

Create an envelope and process it for a log:

```java
EgressEnvelope envelope = EgressEnvelope.builder()
        .put(userId, "user-123")
        .put(email, "user@example.com")
        .put(token, "very-secret-token")
        .build();

EgressReport report =
        EgressProcessor.secureDefaults()
                .process(envelope, EgressSink.LOG, "request-log");

System.out.println(report.getOutput());
```

Expected output:

```text
{user.id=user-123, user.email=[REDACTED]}
```

Policy decisions:

```text
user.id    -> ALLOW
user.email -> REDACT
auth.token -> DENY
```

The credential never enters the approved output.

## Allow one specific use

Positive rules stay scoped to both sink and purpose:

```java
EgressPolicyEngine policy =
        EgressPolicyEngine.builder()
                .add(EgressRules.allow(
                        email,
                        EgressSink.NETWORK,
                        "account-provider",
                        "ACCOUNT_EMAIL_REQUIRED"))
                .withSecureDefaults()
                .build();
```

This allows the email for `NETWORK / account-provider`, not for unrelated purposes such as analytics.

## Dynamic data

Satellite Data works independently from Egress:

```java
SatelliteData data = new SatelliteData();

data.put("caseNumber", "C12.12343");
data.put("retry", 3);
data.put("active", true);

String caseNumber = data.getString("caseNumber");
int retry = data.getInt("retry");
boolean active = data.getBoolean("active");
```

Use `DataDefinition` when fields need structure, defaults or required values.

## Safe integrations

```java
Slf4jEgressLogger.secure(logger);
JacksonEgressSerializer.secure(objectMapper);
OpenTelemetrySpanAdapter.secure();
```

Custom processors remain available when you need your own policies, redactors or tokenizers.

## Security defaults

| Data | Default |
| --- | --- |
| `PUBLIC` | allow |
| `INTERNAL` | keep local; restrict external egress |
| `CONFIDENTIAL` | redact in observability; otherwise deny unless authorized |
| `RESTRICTED` | deny unless explicitly authorized |
| `CREDENTIAL` / `SECRET` | deny |

Satellite also fails closed when no policy matches, requires sink + purpose for positive rules, blocks raw sensitive values in observability, and omits denied values from approved output.

## Modules

| Module | Purpose |
| --- | --- |
| `satellite-data` | dynamic typed data and PMAP/XML |
| `satellite-egress-core` | keys, envelopes, metadata and schemas |
| `satellite-egress-policy` | policy and processing |
| `satellite-egress-observability` | SLF4J |
| `satellite-egress-jackson` | JSON / JSON Schema |
| `satellite-egress-opentelemetry` | OpenTelemetry |
| `satellite-data-egress-bridge` | optional Data → Egress bridge |

Satellite Data and Egress remain independent; only the bridge knows both.

## Documentation

- **[Getting Started](docs/getting-started.md)** — complete examples and expected output
- **[Egress Policy](docs/egress-policy.md)** — rules and custom policies
- **[Security Model](docs/security-model.md)** — classifications and guardrails
- **[Architecture](docs/architecture.md)** — modules and boundaries
- **[Security Policy](SECURITY.md)** — vulnerability reporting

## Build

```bash
mvn --batch-mode --no-transfer-progress verify
```

Satellite 2.x is under active development.

## License

MIT
