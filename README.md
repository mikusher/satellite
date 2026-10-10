# Satellite

**Typed dynamic data and policy-controlled application egress for Java.**

Satellite has two independent sides:

- **Satellite Data** — flexible application data with typed access and optional definitions.
- **Satellite Egress** — classify data and decide what may leave the application, for which sink and purpose.

Use either side independently, or connect them with the optional bridge.

> Current line: `2.0.0-SNAPSHOT` · Java 11 baseline · tested on Java 11, 17 and 21.

## Start here

For complete examples from simple usage to production-style egress policies, including the **expected output for every example**, read:

**[Getting Started — examples and expected output](docs/getting-started.md)**

## 5-minute example

Define the meaning of each outbound value:

```java
Key<String> userId = Key.string("user.id")
        .classifiedAs(DataClassification.PUBLIC);

Key<String> email = Key.string("user.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

Key<String> token = Key.string("auth.token")
        .classifiedAs(DataClassification.RESTRICTED)
        .category(DataCategory.CREDENTIAL);

EgressEnvelope envelope = EgressEnvelope.builder()
        .put(userId, "user-123")
        .put(email, "user@example.com")
        .put(token, "very-secret-token")
        .build();

EgressReport report =
        EgressProcessor.secureDefaults()
                .process(
                        envelope,
                        EgressSink.LOG,
                        "request-log");

System.out.println(report.getOutput());
```

Expected output:

```text
{user.id=user-123, user.email=[REDACTED]}
```

The credential never enters the approved output.

The policy decisions are:

```text
user.id    -> ALLOW
user.email -> REDACT
auth.token -> DENY
```

The important distinction is that `EgressEnvelope` may contain the original values **inside the application**. The policy is evaluated when those values are about to cross an egress boundary.

## Simple by default, detailed when needed

The common path uses short, intention-revealing APIs:

```java
EgressProcessor.secureDefaults()
        .process(envelope, EgressSink.LOG, "request-log");

EgressRules.allow(
        email,
        EgressSink.NETWORK,
        "account-provider",
        "ACCOUNT_EMAIL_REQUIRED");

Slf4jEgressLogger.secure(logger);
JacksonEgressSerializer.secure(objectMapper);
OpenTelemetrySpanAdapter.secure();
```

The lower-level builders and constructors remain available for advanced policies, custom redactors/tokenizers and specialized integrations. The short APIs do not weaken fail-closed behavior, purpose scoping or observability guardrails.

## Mental model

```text
SatelliteData
    |
    | dynamic application data
    v
SatelliteDataEgressBridge       optional
    |
    v
EgressEnvelope
    |
    | type + classification + category
    | origin + trust
    v
EgressPolicyEngine
    |
    +--> ALLOW
    +--> REDACT
    +--> TOKENIZE
    +--> DENY
    |
    v
EgressReport
    |
    v
approved sink adapter
```

An egress decision can depend on:

```text
the data
+ where it is going
+ why it is going there
+ where the value came from
+ how much the application trusts it
```

## Modules

| Module | Purpose |
| --- | --- |
| `satellite-data` | `SatelliteData`, `DataDefinition`, conversions and hardened PMAP/XML |
| `satellite-egress-core` | `Key<T>`, `EgressEnvelope`, runtime metadata and schemas |
| `satellite-egress-policy` | `ALLOW`, `REDACT`, `TOKENIZE`, `DENY` |
| `satellite-egress-observability` | Policy-enforced SLF4J |
| `satellite-egress-jackson` | Policy-enforced JSON + JSON Schema 2020-12 |
| `satellite-egress-opentelemetry` | Policy-enforced OpenTelemetry attributes |
| `satellite-data-egress-bridge` | Optional Satellite Data → Egress bridge |

**Boundary rule:** Satellite Data does not depend on Egress. Egress does not depend on Satellite Data. Only the bridge knows both.

## Secure defaults

The default egress policy is deliberately conservative:

| Data | Default behavior |
| --- | --- |
| `PUBLIC` | Allow |
| `INTERNAL` | Allow for local sinks; redact generic serialization; deny network |
| `CONFIDENTIAL` | Redact in observability; deny other sinks unless explicitly authorized |
| `RESTRICTED` | Deny unless explicitly authorized for an appropriate non-observability use |
| `CREDENTIAL` / `SECRET` | Deny |

Additional guardrails:

- no matching policy fails closed;
- positive `ALLOW` and `TOKENIZE` rules require both a sink and a purpose;
- confidential, restricted, privacy-sensitive, credential and secret values cannot be emitted raw to observability sinks;
- redaction or tokenization failures become `DENY`;
- violation records never contain the protected value;
- the Satellite Data bridge rejects unclassified fields by default;
- PMAP/XML parsing blocks DTD/external entities and applies finite resource limits.

## Documentation

- **[Getting Started](docs/getting-started.md)** — complete examples with expected output
- **[Egress policy](docs/egress-policy.md)** — short rules, custom policies and processing semantics
- **[Architecture](docs/architecture.md)** — module, dependency and API-layer boundaries
- **[Security model](docs/security-model.md)** — classifications, guardrails and threat model
- **[Security policy](SECURITY.md)** — vulnerability reporting and security guarantees

## Build

```bash
mvn --batch-mode --no-transfer-progress verify
```

Expected result:

```text
BUILD SUCCESS
```

The repository includes Java 11/17/21 CI, CodeQL, Dependabot, CycloneDX SBOM generation, dependency review, module-boundary enforcement and guarded release workflows.

## Status

Satellite 2.x is under active development. APIs may still change before the first stable 2.x release.

## License

MIT.
