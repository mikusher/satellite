# Satellite Egress security model

This document describes the current Satellite 2.x security model.

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

`ValueMetadata` records:

- `DataOrigin`
- `TrustLevel`

These belong to a value, not to the key definition. The same logical field may be validated in one flow and untrusted in another.

## Default policy

`DefaultEgressRule` is deliberately conservative.

| Data | Default behavior |
| --- | --- |
| Credential or Secret category | DENY |
| RESTRICTED | DENY |
| CONFIDENTIAL | REDACT in observability; DENY in other sinks |
| Privacy-sensitive category | REDACT in observability; DENY in other sinks |
| INTERNAL | ALLOW local observability/storage-style sinks; REDACT generic serialization; DENY network |
| PUBLIC | ALLOW |

Application-specific rules are evaluated before the default rule.

## Purpose limitation

`PolicyRule` can match purpose together with key, sink, classification, category, origin and trust level.

An authorization for:

```text
key=user.email
sink=NETWORK
purpose=account-provider
```

does not authorize the same value for `purpose=analytics`.

Purpose strings and key names reject control characters.

## Fail-closed behavior

If no rule returns a decision, `EgressPolicyEngine` returns `DENY`.

If `TOKENIZE` is requested without a tokenizer, or redaction/tokenization fails, `EgressProcessor` converts the operation to `DENY`.

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

Each adapter invokes `EgressProcessor` before writing to the destination.

The SLF4J adapter also escapes line breaks and control characters.

## Preventing classification downgrade

`EgressEnvelope.Builder` and `SatelliteSchema.Builder` reject conflicting `Key` definitions that share the same external name.

This prevents a restricted field and a public alias from colliding during egress.

## Satellite Data bridge

`SatelliteData` remains independent from Egress.

The optional `SatelliteDataEgressBridge` requires an explicit registry of typed Egress keys. Strict conversion fails on an unclassified field instead of silently exporting it.

## Threats not solved by Satellite

Satellite is not a substitute for:

- authorization/authentication;
- encryption at rest or in transit;
- secret storage;
- endpoint DLP products;
- database row/column security;
- application input validation;
- legal/privacy governance.

It is an application-level control for semantic data handling and egress.
