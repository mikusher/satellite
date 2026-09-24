# Satellite Egress security model

This document describes the current security model implemented by the 2.x foundation.

## Classification is not category

Satellite intentionally separates disclosure level from semantic data type.

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

A personal-data field can therefore be confidential or restricted without pretending that "PII" is itself a disclosure level.

## Runtime metadata

`ValueMetadata` records:

- `DataOrigin`
- `TrustLevel`

These belong to a value, not to the key definition. For example, `search.term` may be validated in one request and untrusted in another.

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

`PolicyRule` can match a concrete purpose in addition to key, sink, classification, category, origin and trust level.

An explicit authorization for:

```text
key=user.email
sink=NETWORK
purpose=account-provider
```

does not authorize the same value for `purpose=analytics`.

Purpose strings and key names reject control characters.

## Fail-closed behavior

If no rule returns a decision, `EgressPolicyEngine` returns `DENY`.

If a rule requests `TOKENIZE` without a configured tokenizer, `EgressProcessor` converts that outcome to `DENY` and records a violation.

Denied values are not present in the output map.

## Pseudonymization

`HmacSha256Tokenizer` provides deterministic pseudonymization for values that need stable correlation.

Properties:

- HMAC-SHA-256;
- minimum 32-byte application-supplied key;
- key name is included before the value, providing domain separation between fields;
- original value is not embedded in the token;
- unsupported complex objects are rejected rather than serialized ambiguously.

This is pseudonymization, not anonymization.

## PrivacyViolation

A violation contains only metadata required to explain the policy decision:

- key name;
- sink;
- purpose;
- reason code;
- classification;
- categories;
- origin;
- trust level.

It intentionally does not contain the protected value.

## Adapter boundary

The supported adapters accept `SatelliteMap` rather than arbitrary maps:

- SLF4J;
- Jackson;
- OpenTelemetry.

Each adapter calls `EgressProcessor` before writing to the destination.

The SLF4J adapter additionally escapes line breaks and control characters in emitted text.

## Preventing classification downgrade

`SatelliteMap.Builder` and `SatelliteSchema.Builder` reject conflicting `Key` definitions that share the same external name.

This prevents an envelope from containing, for example, both a restricted `token` definition and a public `token` alias that could collide during egress.

## Legacy ParameterMap

ParameterMap remains independent.

The optional bridge requires an explicit registry of typed Egress keys. Strict conversion fails on an unclassified legacy field instead of silently exporting it.

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
