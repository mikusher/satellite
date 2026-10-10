# Satellite Egress security model

This document describes the current Satellite 2.x security model and the invariants that the ergonomic API must preserve.

## Security boundary

Satellite treats application egress as an explicit policy boundary.

```text
classified value
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
approved output only
```

Shortcuts such as `EgressProcessor.secureDefaults()`, `EgressRules.allow(...)` and adapter `secure(...)` factories all enter this same pipeline. They do not bypass it.

## Classification is not category

Satellite separates disclosure level from semantic data type.

### DataClassification

- `PUBLIC`
- `INTERNAL`
- `CONFIDENTIAL`
- `RESTRICTED`

### DataCategory

Examples include:

- `PERSONAL_DATA`
- `CREDENTIAL`
- `SECRET`
- `FINANCIAL`
- `HEALTH`
- `LOCATION`
- device/network identifiers

A personal-data field can therefore be confidential or restricted without treating PII as a disclosure level.

## Runtime metadata

Runtime metadata records:

- `DataOrigin`
- `TrustLevel`

These belong to a value, not to the key definition. The same logical field may be validated in one flow and untrusted in another.

The common API writes metadata directly:

```java
EgressEnvelope envelope = EgressEnvelope.builder()
        .put(
                email,
                "user@example.com",
                DataOrigin.DATABASE,
                TrustLevel.VALIDATED)
        .build();
```

`ValueMetadata` remains available as the explicit metadata object.

## Default policy

`EgressPolicyEngine.secureDefaults()` and `EgressProcessor.secureDefaults()` use Satellite's conservative fallback.

| Data | Default behavior |
| --- | --- |
| Credential or Secret category | DENY |
| RESTRICTED | DENY |
| CONFIDENTIAL | REDACT in observability; DENY in other sinks |
| Privacy-sensitive category | REDACT in observability; DENY in other sinks |
| INTERNAL | ALLOW local observability/storage-style sinks; REDACT generic serialization; DENY network |
| PUBLIC | ALLOW |

For custom policies, applications should use:

```java
EgressPolicyEngine policy =
        EgressPolicyEngine.builder()
                .add(applicationRule)
                .withSecureDefaults()
                .build();
```

This appends the secure fallback after explicit rules.

## Purpose limitation

A positive egress authorization is scoped by both sink and purpose.

The short API makes this visible:

```java
EgressRules.allow(
        email,
        EgressSink.NETWORK,
        "account-provider",
        "ACCOUNT_EMAIL_REQUIRED");
```

An authorization for:

```text
key=user.email
sink=NETWORK
purpose=account-provider
```

does not authorize the same value for `purpose=analytics`.

Purpose strings and key names reject control characters.

## Observability is non-bypassable

An explicit `ALLOW` cannot emit raw confidential, restricted, secret, credential or privacy-sensitive data into observability sinks.

This applies to:

- logs;
- traces;
- metrics;
- audit output.

For example, a rule that requests raw confidential email in a log still resolves to:

```text
DENY [OBSERVABILITY_RAW_SENSITIVE_DENIED]
```

The rule may instead redact, tokenize or deny the value.

This guardrail applies equally to `EgressRules.allow(...)` and detailed `PolicyRule` definitions.

## Fail-closed behavior

If no rule returns a decision, `EgressPolicyEngine` returns:

```text
DENY [NO_MATCHING_POLICY]
```

If redaction fails:

```text
DENY [REDACTION_FAILED]
```

If tokenization is requested without a tokenizer:

```text
DENY [TOKENIZER_NOT_CONFIGURED]
```

If tokenization fails:

```text
DENY [TOKENIZATION_FAILED]
```

Denied values never appear in the approved output.

## Pseudonymization

`HmacSha256Tokenizer` provides deterministic pseudonymization for values that need stable correlation.

Properties:

- HMAC-SHA-256;
- minimum 32-byte application-supplied key;
- key-name domain separation;
- original value is not embedded in the token;
- unsupported complex objects are rejected.

This is pseudonymization, not anonymization.

The HMAC key should come from a real secret-management system.

## PrivacyViolation

A violation contains only metadata needed to explain the decision:

- key name;
- sink;
- purpose;
- reason code;
- classification;
- categories;
- origin;
- trust level.

It never contains the protected value.

## Adapter boundary

Supported adapters accept `EgressEnvelope`:

- SLF4J;
- Jackson;
- OpenTelemetry.

The common path uses:

```java
Slf4jEgressLogger.secure(logger);
JacksonEgressSerializer.secure(objectMapper);
OpenTelemetrySpanAdapter.secure();
```

Those factories use the same secure-default processor as direct Egress processing.

Applications that need a custom policy pass an explicit `EgressProcessor` to the adapter constructor.

The SLF4J adapter also escapes line breaks and control characters.

## Preventing classification downgrade

`EgressEnvelope.Builder` and `SatelliteSchema.Builder` reject conflicting `Key` definitions that share the same external name.

This prevents a restricted field and a public alias from colliding during egress.

## Satellite Data bridge

`SatelliteData` remains independent from Egress.

The optional `SatelliteDataEgressBridge` is the explicit conversion boundary.

Strict schema-backed conversion:

```java
DataBridgeResult result =
        SatelliteDataEgressBridge.toEnvelope(
                data,
                schema,
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED);
```

provides these guarantees:

- every source field must have an explicit classified key;
- unknown fields are rejected;
- required schema fields are validated;
- key type checks still apply;
- runtime origin/trust metadata is attached during conversion.

Lenient conversion must be requested explicitly and reports ignored source fields.

## Ergonomics do not weaken invariants

The convenience layer intentionally does **not** add:

- `allow(key)` without sink and purpose;
- `process(envelope, sink)` without purpose;
- raw map export from `EgressEnvelope`;
- automatic classification based on field names;
- permissive fallback rules;
- implicit tokenizer secrets;
- implicit lenient bridge conversion.

Satellite's short API removes ceremony while keeping the security-relevant context explicit.

## Threats not solved by Satellite

Satellite is not a substitute for:

- authorization/authentication;
- encryption at rest or in transit;
- secret storage;
- endpoint DLP products;
- database row/column security;
- application input validation;
- legal/privacy governance;
- full taint/data-flow analysis.

It is an application-level control for semantic data handling and egress.
