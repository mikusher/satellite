# Egress policy model

Satellite Egress is an in-process application egress enforcement layer.

It is not a post-processing log masker and it is not a network DLP appliance. The intended enforcement point is immediately before application data is sent to a log, trace, serializer, network request or persistence sink.

## Start with the short API

For the common case, use Satellite's secure defaults directly:

```java
EgressReport report =
        EgressProcessor.secureDefaults()
                .process(
                        envelope,
                        EgressSink.LOG,
                        "request-log");
```

This is equivalent to constructing a processor with `EgressPolicyEngine.secureDefaults()`, but keeps the normal path concise.

The `purpose` remains explicit. Satellite deliberately does not provide a raw `process(envelope, sink)` shortcut because purpose is part of the authorization boundary.

## Why classification and category are separate

A classification answers **how restricted is this value?**

- `PUBLIC`
- `INTERNAL`
- `CONFIDENTIAL`
- `RESTRICTED`

A category answers **what kind of data is it?**

Examples include `PERSONAL_DATA`, `CREDENTIAL`, `SECRET`, `FINANCIAL`, `HEALTH` and `LOCATION`.

PII is therefore not treated as a higher level in the classification hierarchy.

## Why origin and trust are per value

Origin and trust can change for the same logical field.

For example, `user.id` may be loaded from a validated database record in one flow and directly from an untrusted HTTP parameter in another. Encoding origin/trust on `Key<T>` would incorrectly make them static.

The concise envelope form is:

```java
EgressEnvelope envelope = EgressEnvelope.builder()
        .put(
                userId,
                "user-123",
                DataOrigin.DATABASE,
                TrustLevel.VALIDATED)
        .build();
```

`ValueMetadata.of(...)` remains available when the application already has a metadata object.

## Convenience rules

For key-specific policies, prefer `EgressRules`.

### Allow

```java
EgressRules.allow(
        email,
        EgressSink.NETWORK,
        "account-provider",
        "ACCOUNT_EMAIL_REQUIRED");
```

### Tokenize

```java
EgressRules.tokenize(
        customerId,
        EgressSink.STORAGE,
        "analytics",
        "ANALYTICS_PSEUDONYM");
```

Both positive actions require a sink **and** a purpose.

There is intentionally no convenience API equivalent to:

```text
allow this key everywhere
```

### Redact and deny

Restrictive actions may apply to an entire sink:

```java
EgressRules.redact(
        email,
        EgressSink.LOG,
        "EMAIL_REDACTED");

EgressRules.deny(
        internalRecord,
        EgressSink.NETWORK,
        "INTERNAL_NETWORK_DENIED");
```

They can also be purpose-scoped when necessary.

## Custom policies

Use `PolicyRule.builder()` when a rule must match richer semantics:

```java
PolicyRule rule =
        PolicyRule.builder()
                .category(DataCategory.PERSONAL_DATA)
                .origin(DataOrigin.USER_INPUT)
                .trustLevel(TrustLevel.UNTRUSTED)
                .sink(EgressSink.LOG)
                .action(EgressAction.REDACT)
                .reasonCode("UNTRUSTED_PII_IN_LOG")
                .build();
```

Build a custom policy like this:

```java
EgressPolicyEngine policy =
        EgressPolicyEngine.builder()
                .add(rule)
                .add(
                        EgressRules.allow(
                                email,
                                EgressSink.NETWORK,
                                "account-provider",
                                "ACCOUNT_EMAIL_REQUIRED"))
                .withSecureDefaults()
                .build();

EgressProcessor processor =
        new EgressProcessor(policy);
```

`withSecureDefaults()` appends the conservative fallback **after** the explicit application rules. Callers do not need to instantiate `DefaultEgressRule` directly.

If `withSecureDefaults()` is omitted and no configured rule matches, the engine still returns `DENY` with `NO_MATCHING_POLICY`.

## Secure defaults

The secure default behavior is deliberately conservative:

| Data | Default behavior |
| --- | --- |
| Credential or Secret category | DENY |
| RESTRICTED | DENY |
| CONFIDENTIAL | REDACT in observability; DENY in other sinks |
| Privacy-sensitive category | REDACT in observability; DENY in other sinks |
| INTERNAL | ALLOW local observability/storage-style sinks; REDACT generic serialization; DENY network |
| PUBLIC | ALLOW |

Application-specific rules are evaluated before the secure fallback when `withSecureDefaults()` is used.

## Non-bypassable observability guardrail

An explicit `ALLOW` cannot force raw sensitive data into:

- `LOG`;
- `TRACE`;
- `METRIC`;
- `AUDIT`.

For example:

```java
EgressPolicyEngine policy =
        EgressPolicyEngine.builder()
                .add(
                        EgressRules.allow(
                                email,
                                EgressSink.LOG,
                                "debug",
                                "DEBUG_EMAIL"))
                .withSecureDefaults()
                .build();
```

If `email` is confidential or privacy-sensitive, the final decision becomes:

```text
DENY [OBSERVABILITY_RAW_SENSITIVE_DENIED]
```

The convenience API therefore cannot bypass the same guardrail enforced for detailed `PolicyRule` definitions.

## Structured values require classification

An explicit `ALLOW` is not permission to export an unclassified object graph. Raw `Map`, `List`, array and arbitrary POJO values are denied with `UNCLASSIFIED_COMPLEX_VALUE_DENIED`, even when the outer key is `PUBLIC`. Classify each outbound leaf with its own `Key<T>` instead.

```java
Key<String> orderId = Key.string("order.id")
        .classifiedAs(DataClassification.PUBLIC);

Key<String> customerEmail = Key.string("customer.email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

EgressEnvelope envelope = EgressEnvelope.builder()
        .put(orderId, "ORDER-9001")
        .put(customerEmail, "customer@example.com")
        .build();
```

## Extension failure behavior

A custom `EgressRule` that throws or returns null fails closed (`POLICY_EVALUATION_FAILED` or `INVALID_POLICY_RESULT`). No exception message containing protected data is included in violations.

The `Redactor` interface returns a `String`. Custom redaction and tokenization results are rejected if they are null, blank, contain control characters or simply echo the raw scalar input. The respective decisions are `INVALID_REDACTION_OUTPUT` and `INVALID_TOKEN_OUTPUT`.

Built-in transforms are preferable to ad hoc implementations, because it is not possible to automatically prove that an arbitrary replacement string contains no sensitive information.

## Processing failures fail closed

If redaction throws, the processor changes the action to:

```text
DENY [REDACTION_FAILED]
```

If tokenization is requested without a tokenizer:

```text
DENY [TOKENIZER_NOT_CONFIGURED]
```

If tokenization throws:

```text
DENY [TOKENIZATION_FAILED]
```

Denied values never enter the approved output.

## Violation reporting

A denied output can produce `PrivacyViolation` metadata containing:

- external key name;
- sink;
- purpose;
- reason code;
- classification;
- categories;
- origin;
- trust level.

It never contains the protected value.

## Tokenization

`HmacSha256Tokenizer` supports deterministic pseudonymization for correlation use cases. It requires at least 32 bytes of secret material and domain-separates tokens by key name.

A processor with tokenization can be configured explicitly:

```java
EgressProcessor processor =
        new EgressProcessor(
                policy,
                new ConstantRedactor(),
                new HmacSha256Tokenizer(hmacSecret));
```

The HMAC key should come from a real secret-management system and should not be hard-coded in source control.

## Adapter shortcuts

The common adapter path uses secure factories:

```java
Slf4jEgressLogger logger =
        Slf4jEgressLogger.secure(slf4jLogger);

JacksonEgressSerializer serializer =
        JacksonEgressSerializer.secure(objectMapper);

OpenTelemetrySpanAdapter telemetry =
        OpenTelemetrySpanAdapter.secure();
```

Each factory uses the same secure-default processor. Explicit constructors remain available when an application needs a custom policy.

## API principle

Satellite's ergonomic API removes ceremony, not security context.

The following remain intentionally explicit:

- classification and category on keys;
- purpose for every processed egress operation;
- sink + purpose for positive rules;
- custom tokenizer configuration;
- lenient bridge conversion;
- advanced matchers such as origin/trust.
