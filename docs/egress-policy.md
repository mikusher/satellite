# Egress policy model

Satellite Egress is an in-process application egress enforcement layer.

It is not a post-processing log masker and it is not a network DLP appliance. The intended enforcement point is immediately before application data is sent to a log, trace, serializer, network request or persistence sink.

## Why classification and category are separate

A classification answers **how restricted is this value?**

- PUBLIC
- INTERNAL
- CONFIDENTIAL
- RESTRICTED

A category answers **what kind of data is it?**

Examples include PERSONAL_DATA, CREDENTIAL, FINANCIAL, HEALTH and LOCATION.

PII is therefore not treated as a "higher level" in the classification hierarchy.

## Why origin and trust are per value

Origin and trust can change for the same logical field.

For example `user.id` may be loaded from a validated database record in one flow and directly from an untrusted HTTP parameter in another. Encoding origin/trust on `Key<T>` would incorrectly make them static.

## Secure defaults

`EgressPolicyEngine.secureDefaults()` is intentionally conservative.

Explicit application rules should be narrow and should be inserted before `DefaultEgressRule`.

## Violation reporting

A denied output produces `PrivacyViolation` metadata containing:

- external key name;
- sink;
- reason code;
- classification;
- categories;
- origin;
- trust level.

It never contains the protected value.

## Tokenization

`HmacSha256Tokenizer` supports deterministic pseudonymization for correlation use cases. It requires at least 32 bytes of secret material and domain-separates tokens by key name.

The HMAC key should come from a real secret-management system and should not be hard-coded in source control.
