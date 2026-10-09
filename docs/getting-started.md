# Getting Started with Satellite 2

This guide shows Satellite from the simplest use of `SatelliteData` to a complete application-level egress policy.

Every section follows the same pattern:

```text
Code
→ Expected output
→ Why it matters
```

Satellite uses **progressive disclosure**: the common path is intentionally short, while the detailed builders remain available for advanced policy matching and customization.

```text
common case    -> secure factories + convenience rules
advanced case  -> PolicyRule builder + custom processor components
```

## 1. SatelliteData — dynamic typed application data

Use `SatelliteData` when you want flexible application data with typed getters.

```java
import com.mikusher.parameter.SatelliteData;

public class SimpleDataExample {

    public static void main(String[] args) throws Exception {
        SatelliteData data = new SatelliteData();

        data.put("caseNumber", "C12.12343");
        data.put("retry", 3);
        data.put("active", true);

        System.out.println("caseNumber = " + data.getString("caseNumber"));
        System.out.println("retry = " + data.getInt("retry"));
        System.out.println("active = " + data.getBoolean("active"));
    }
}
```

Expected output:

```text
caseNumber = C12.12343
retry = 3
active = true
```

At this level Satellite is simply dynamic application data plus typed access. Egress security is completely optional.

---

## 2. SatelliteData with DataDefinition

Use `DataDefinition` when the dynamic data should have a known structure.

```java
import com.mikusher.parameter.DataDefinition;
import com.mikusher.parameter.SatelliteData;

public class DefinedDataExample {

    public static void main(String[] args) throws Exception {
        DataDefinition definition =
                new DataDefinition("request", "Incoming request");

        definition.addString("requestId", "Request identifier");
        definition.addInteger("retry", "Retry number");

        SatelliteData data = new SatelliteData(definition);

        data.put("requestId", "REQ-1001");
        data.put("retry", 2);

        System.out.println("requestId = " + data.getString("requestId"));
        System.out.println("retry = " + data.getInt("retry"));
    }
}
```

Expected output:

```text
requestId = REQ-1001
retry = 2
```

Mental model:

```text
SatelliteData  = runtime values
DataDefinition = expected fields, types, requirements and defaults
```

---

## 3. Define security-aware keys

Egress starts with `Key<T>`. A key owns the static meaning of a value.

```java
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;

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

Expected definitions:

```text
user.id           -> String / PUBLIC / required
user.email        -> String / CONFIDENTIAL / PERSONAL_DATA
auth.access_token -> String / RESTRICTED / CREDENTIAL
```

Classification answers **how restricted** the value is. Category answers **what kind of value** it is.

---

## 4. Build an EgressEnvelope

`EgressEnvelope` contains typed values plus security metadata.

```java
import io.github.mikusher.satellite.egress.*;

String token = "eyJhbGciOi...";

EgressEnvelope envelope = EgressEnvelope.builder()
        .put(userId, "user-123")
        .put(
                email,
                "user@example.com",
                DataOrigin.DATABASE,
                TrustLevel.VALIDATED)
        .put(
                accessToken,
                token,
                DataOrigin.HTTP_HEADER,
                TrustLevel.UNTRUSTED)
        .build();
```

Expected logical content:

```text
user.id           = user-123
user.email        = user@example.com   [DATABASE / VALIDATED]
auth.access_token = eyJhbGciOi...      [HTTP_HEADER / UNTRUSTED]
```

Nothing has been redacted yet.

The envelope represents classified data **inside the application**. Policy is evaluated when data is about to leave.

---

## 5. Secure defaults — ALLOW, REDACT and DENY

```java
import io.github.mikusher.satellite.egress.policy.*;

EgressReport report =
        EgressProcessor.secureDefaults()
                .process(
                        envelope,
                        EgressSink.LOG,
                        "request-log");

System.out.println("Output: " + report.getOutput());

for (EgressDecisionRecord decision : report.getDecisions()) {
    System.out.println(
            decision.getKeyName()
                    + " -> "
                    + decision.getAction()
                    + " ["
                    + decision.getReasonCode()
                    + "]");
}
```

Expected output:

```text
Output: {user.id=user-123, user.email=[REDACTED]}
user.id -> ALLOW [PUBLIC_DATA_ALLOWED]
user.email -> REDACT [SENSITIVE_DATA_REDACTED]
auth.access_token -> DENY [SECRET_CATEGORY_DENIED]
```

The original credential is absent from `report.getOutput()`.

---

## 6. Runtime origin and trust

A value can be public but still unsafe to log raw because it came directly from untrusted input.

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.policy.*;

public class RuntimeMetadataExample {

    public static void main(String[] args) {
        Key<String> search =
                Key.string("search.term")
                        .classifiedAs(DataClassification.PUBLIC);

        EgressEnvelope input =
                EgressEnvelope.builder()
                        .put(
                                search,
                                "admin\nDROP TABLE users",
                                DataOrigin.USER_INPUT,
                                TrustLevel.UNTRUSTED)
                        .build();

        EgressPolicyEngine policy =
                EgressPolicyEngine.builder()
                        .add(
                                PolicyRule.builder()
                                        .origin(DataOrigin.USER_INPUT)
                                        .trustLevel(TrustLevel.UNTRUSTED)
                                        .sink(EgressSink.LOG)
                                        .action(EgressAction.REDACT)
                                        .reasonCode("UNTRUSTED_INPUT_REDACTED")
                                        .build())
                        .withSecureDefaults()
                        .build();

        EgressReport result =
                new EgressProcessor(policy)
                        .process(
                                input,
                                EgressSink.LOG,
                                "diagnostics");

        System.out.println(result.getOutput());
        System.out.println(result.getDecisions().get(0).getReasonCode());
    }
}
```

Expected output:

```text
{search.term=[REDACTED]}
UNTRUSTED_INPUT_REDACTED
```

The decision can therefore depend on both static semantics and runtime context.

---

## 7. Allow sensitive data only for one purpose

Positive rules are deliberately narrow. `ALLOW` and `TOKENIZE` require both a sink and a purpose.

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.policy.*;

public class PurposeExample {

    public static void main(String[] args) {
        Key<String> email =
                Key.string("user.email")
                        .classifiedAs(DataClassification.CONFIDENTIAL)
                        .category(DataCategory.PERSONAL_DATA);

        EgressEnvelope envelope =
                EgressEnvelope.builder()
                        .put(email, "user@example.com")
                        .build();

        EgressPolicyEngine policy =
                EgressPolicyEngine.builder()
                        .add(
                                EgressRules.allow(
                                        email,
                                        EgressSink.NETWORK,
                                        "account-provider",
                                        "ACCOUNT_EMAIL_REQUIRED"))
                        .withSecureDefaults()
                        .build();

        EgressProcessor processor = new EgressProcessor(policy);

        EgressReport accountProvider =
                processor.process(
                        envelope,
                        EgressSink.NETWORK,
                        "account-provider");

        EgressReport analytics =
                processor.process(
                        envelope,
                        EgressSink.NETWORK,
                        "analytics");

        System.out.println("Account provider: " + accountProvider.getOutput());
        System.out.println("Analytics: " + analytics.getOutput());
    }
}
```

Expected output:

```text
Account provider: {user.email=user@example.com}
Analytics: {}
```

The same value is allowed for the account provider and denied for analytics.

---

## 8. Deterministic pseudonymization with HMAC

Use `TOKENIZE` when a downstream system needs stable correlation without receiving the original identifier.

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.policy.*;

import java.nio.charset.StandardCharsets;

public class TokenizationExample {

    public static void main(String[] args) {
        Key<String> customerId =
                Key.string("customer.id")
                        .classifiedAs(DataClassification.CONFIDENTIAL);

        EgressEnvelope envelope =
                EgressEnvelope.builder()
                        .put(customerId, "45783910")
                        .build();

        EgressPolicyEngine policy =
                EgressPolicyEngine.builder()
                        .add(
                                EgressRules.tokenize(
                                        customerId,
                                        EgressSink.STORAGE,
                                        "analytics",
                                        "ANALYTICS_PSEUDONYM"))
                        .withSecureDefaults()
                        .build();

        byte[] secret =
                "0123456789abcdef0123456789abcdef"
                        .getBytes(StandardCharsets.UTF_8);

        EgressProcessor processor =
                new EgressProcessor(
                        policy,
                        new ConstantRedactor(),
                        new HmacSha256Tokenizer(secret));

        EgressReport result =
                processor.process(
                        envelope,
                        EgressSink.STORAGE,
                        "analytics");

        System.out.println(result.getOutput());
    }
}
```

Expected output for the secret and value above:

```text
{customer.id=hmac-sha256:rAGcxpTV0WFFrBEns91gj17fhB6lR3uTo4EY3y7te5g}
```

`45783910` is not exported.

The same key name, value and HMAC secret produce the same token. In production the secret should come from a proper secrets/KMS system.

---

## 9. Safe SLF4J logging

The logging adapter policy-processes the envelope before producing the log line and escapes control characters.

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.observability.*;
import io.github.mikusher.satellite.egress.policy.*;

import org.slf4j.LoggerFactory;

public class LoggingExample {

    public static void main(String[] args) {
        Key<String> status =
                Key.string("status")
                        .classifiedAs(DataClassification.PUBLIC);

        Key<String> email =
                Key.string("email")
                        .classifiedAs(DataClassification.CONFIDENTIAL)
                        .category(DataCategory.PERSONAL_DATA);

        Key<String> token =
                Key.string("token")
                        .classifiedAs(DataClassification.RESTRICTED)
                        .category(DataCategory.CREDENTIAL);

        EgressEnvelope envelope =
                EgressEnvelope.builder()
                        .put(status, "line1\nline2")
                        .put(email, "user@example.com")
                        .put(token, "DO-NOT-LOG")
                        .build();

        Slf4jEgressLogger logger =
                Slf4jEgressLogger.secure(
                        LoggerFactory.getLogger(LoggingExample.class));

        SafeLogEvent event =
                logger.prepare(
                        "request\ncomplete",
                        envelope,
                        "request-log");

        System.out.println(event.getLine());
    }
}
```

Expected output:

```text
request\ncomplete {email=[REDACTED], status=line1\nline2}
```

Neither `user@example.com` nor `DO-NOT-LOG` appears raw.

---

## 10. Safe JSON serialization

```java
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.jackson.*;
import io.github.mikusher.satellite.egress.policy.*;

public class JsonExample {

    public static void main(String[] args) throws Exception {
        Key<String> status =
                Key.string("status")
                        .classifiedAs(DataClassification.PUBLIC);

        Key<String> internal =
                Key.string("internal.note");

        Key<String> secret =
                Key.string("auth.token")
                        .classifiedAs(DataClassification.RESTRICTED)
                        .category(DataCategory.CREDENTIAL);

        EgressEnvelope envelope =
                EgressEnvelope.builder()
                        .put(status, "ok")
                        .put(internal, "internal-only")
                        .put(secret, "never-serialize")
                        .build();

        JacksonEgressSerializer serializer =
                JacksonEgressSerializer.secure(
                        new ObjectMapper());

        String json =
                serializer.toJson(
                        envelope,
                        "api-response");

        System.out.println(json);
    }
}
```

Expected output:

```json
{"status":"ok","internal.note":"[REDACTED]"}
```

The restricted credential is completely absent from the JSON.

---

## 11. OpenTelemetry attributes

The same policy model applies to traces.

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.opentelemetry.*;
import io.github.mikusher.satellite.egress.policy.*;

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
        .put(token, "do-not-trace")
        .build();

OpenTelemetrySpanAdapter adapter =
        OpenTelemetrySpanAdapter.secure();

OpenTelemetryEgressResult result =
        adapter.prepareAttributes(
                envelope,
                "request-trace");
```

Expected logical attributes:

```text
user.id=user-123
user.email=[REDACTED]
auth.token=<absent>
```

The adapter can also apply the approved attributes directly with `applyToSpan(...)`.

---

## 12. Schema validation

```java
import io.github.mikusher.satellite.egress.*;

public class SchemaExample {

    public static void main(String[] args) {
        Key<String> userId =
                Key.string("user.id")
                        .classifiedAs(DataClassification.PUBLIC);

        Key<String> email =
                Key.string("user.email")
                        .classifiedAs(DataClassification.CONFIDENTIAL)
                        .category(DataCategory.PERSONAL_DATA);

        SatelliteSchema schema =
                SatelliteSchema.builder("User")
                        .required(userId)
                        .optional(email)
                        .build();

        EgressEnvelope valid =
                EgressEnvelope.builder()
                        .put(userId, "user-123")
                        .put(email, "user@example.com")
                        .build();

        System.out.println(
                "Valid = "
                        + schema.validate(valid).isValid());

        EgressEnvelope invalid =
                EgressEnvelope.builder()
                        .put(email, "user@example.com")
                        .build();

        ValidationResult result =
                schema.validate(invalid);

        System.out.println("Valid = " + result.isValid());

        for (ValidationError error : result.getErrors()) {
            System.out.println(error);
        }
    }
}
```

Expected output:

```text
Valid = true
Valid = false
REQUIRED_VALUE_MISSING(user.id): Required value is missing
```

Unknown keys are also rejected by default unless `allowUnknownKeys(true)` is explicitly configured.

---

## 13. JSON Schema 2020-12

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.jackson.*;

Key<String> id = Key.string("id")
        .classifiedAs(DataClassification.PUBLIC)
        .required();

Key<String> email = Key.string("email")
        .classifiedAs(DataClassification.CONFIDENTIAL)
        .category(DataCategory.PERSONAL_DATA);

SatelliteSchema schema = SatelliteSchema.builder("User")
        .required(id)
        .optional(email)
        .build();

JsonNode json =
        new JsonSchemaExporter(
                new ObjectMapper())
                .export(schema);

System.out.println(
        new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(json));
```

Expected structure:

```json
{
  "$schema" : "https://json-schema.org/draft/2020-12/schema",
  "title" : "User",
  "type" : "object",
  "additionalProperties" : false,
  "properties" : {
    "id" : {
      "type" : "string",
      "x-satellite-classification" : "PUBLIC",
      "x-satellite-java-type" : "java.lang.String",
      "x-satellite-categories" : [ "GENERAL" ]
    },
    "email" : {
      "type" : "string",
      "x-satellite-classification" : "CONFIDENTIAL",
      "x-satellite-java-type" : "java.lang.String",
      "x-satellite-categories" : [ "PERSONAL_DATA" ]
    }
  },
  "required" : [ "id" ]
}
```

The importer accepts Satellite's supported flat-object subset and rejects ambiguous or unsupported definitions rather than guessing.

---

## 14. Bridge SatelliteData to Egress

The bridge is the explicit boundary between flexible application data and classified egress data. When a `SatelliteSchema` already exists, it can be reused directly as the classified key registry and its required fields are validated during conversion.

```java
import com.mikusher.parameter.SatelliteData;

import io.github.mikusher.satellite.bridge.*;
import io.github.mikusher.satellite.egress.*;

public class BridgeExample {

    public static void main(String[] args) {
        SatelliteData data = new SatelliteData();

        data.put("user.id", "user-123");
        data.put("user.email", "user@example.com");

        Key<String> userId =
                Key.string("user.id")
                        .classifiedAs(DataClassification.PUBLIC);

        Key<String> email =
                Key.string("user.email")
                        .classifiedAs(DataClassification.CONFIDENTIAL)
                        .category(DataCategory.PERSONAL_DATA);

        SatelliteSchema schema =
                SatelliteSchema.builder("User")
                        .required(userId)
                        .optional(email)
                        .build();

        DataBridgeResult result =
                SatelliteDataEgressBridge.toEnvelope(
                        data,
                        schema,
                        DataOrigin.APPLICATION,
                        TrustLevel.VALIDATED);

        EgressEnvelope envelope = result.getEnvelope();

        System.out.println("user.id = " + envelope.get(userId));
        System.out.println("user.email = " + envelope.get(email));
        System.out.println("ignored = " + result.getIgnoredKeys());
    }
}
```

Expected output:

```text
user.id = user-123
user.email = user@example.com
ignored = []
```

Strict mode rejects any field that was not explicitly classified:

```java
data.put("forgotten-secret", "SECRET");

SatelliteDataEgressBridge.toEnvelope(
        data,
        schema,
        DataOrigin.UNKNOWN,
        TrustLevel.UNKNOWN);
```

Expected result:

```text
IllegalArgumentException:
Unclassified SatelliteData key: forgotten-secret
```

There is an explicit lenient migration mode:

```java
DataBridgeResult lenient =
        SatelliteDataEgressBridge.toEnvelopeLenient(
                data,
                schema,
                DataOrigin.UNKNOWN,
                TrustLevel.UNKNOWN);

System.out.println(lenient.getIgnoredKeys());
```

Expected output:

```text
[forgotten-secret]
```

Use lenient mode only when intentionally migrating existing dynamic data.

---

## 15. Hardened PMAP/XML parsing

```java
import com.mikusher.formats.PMapParserLimits;
import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.parameter.SatelliteData;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XmlExample {

    public static void main(String[] args) throws Exception {
        PMapParserLimits limits =
                PMapParserLimits.builder()
                        .maxInputBytes(1024)
                        .maxInputCharacters(1024)
                        .maxDepth(10)
                        .maxEntries(100)
                        .maxCollectionSize(100)
                        .maxTextLength(256)
                        .build();

        StreamedPMapParser parser =
                new StreamedPMapParser(limits);

        String xml =
                "<m>"
                        + "<s n=\"status\">ok</s>"
                        + "<i n=\"retry\">3</i>"
                        + "</m>";

        SatelliteData data =
                parser.getData(
                        new ByteArrayInputStream(
                                xml.getBytes(
                                        StandardCharsets.UTF_8)));

        System.out.println("status = " + data.getString("status"));
        System.out.println("retry = " + data.getInt("retry"));
    }
}
```

Expected output:

```text
status = ok
retry = 3
```

The parser rejects DTD/external entities and enforces input-size, depth, entry-count, collection-size and text-length limits.

---

# Complete example — payment application

This example shows the core Satellite idea: **the same value can have different outcomes depending on the sink and purpose.**

Requirements:

```text
LOG / payment-log
    order.id       -> ALLOW
    customer.email -> REDACT
    payment.token  -> DENY

NETWORK / payment-provider
    order.id       -> ALLOW
    customer.email -> ALLOW
    payment.token  -> ALLOW

NETWORK / analytics
    order.id       -> ALLOW
    customer.email -> DENY
    payment.token  -> DENY
```

Code:

```java
import io.github.mikusher.satellite.egress.*;
import io.github.mikusher.satellite.egress.policy.*;

public class PaymentExample {

    public static void main(String[] args) {
        Key<String> orderId =
                Key.string("order.id")
                        .classifiedAs(DataClassification.PUBLIC);

        Key<String> email =
                Key.string("customer.email")
                        .classifiedAs(DataClassification.CONFIDENTIAL)
                        .category(DataCategory.PERSONAL_DATA);

        Key<String> paymentToken =
                Key.string("payment.token")
                        .classifiedAs(DataClassification.RESTRICTED)
                        .category(DataCategory.FINANCIAL);

        EgressEnvelope envelope =
                EgressEnvelope.builder()
                        .put(orderId, "ORDER-9001")
                        .put(
                                email,
                                "alice@example.com",
                                ValueMetadata.of(
                                        DataOrigin.DATABASE,
                                        TrustLevel.VALIDATED))
                        .put(
                                paymentToken,
                                "tok_very_secret",
                                ValueMetadata.of(
                                        DataOrigin.HTTP_BODY,
                                        TrustLevel.VALIDATED))
                        .build();

        EgressPolicyEngine policy =
                EgressPolicyEngine.builder()
                        .add(
                                EgressRules.allow(
                                        email,
                                        EgressSink.NETWORK,
                                        "payment-provider",
                                        "PAYMENT_EMAIL_REQUIRED"))
                        .add(
                                EgressRules.allow(
                                        paymentToken,
                                        EgressSink.NETWORK,
                                        "payment-provider",
                                        "PAYMENT_TOKEN_REQUIRED"))
                        .withSecureDefaults()
                        .build();

        EgressProcessor processor = new EgressProcessor(policy);

        EgressReport log =
                processor.process(
                        envelope,
                        EgressSink.LOG,
                        "payment-log");

        EgressReport paymentProvider =
                processor.process(
                        envelope,
                        EgressSink.NETWORK,
                        "payment-provider");

        EgressReport analytics =
                processor.process(
                        envelope,
                        EgressSink.NETWORK,
                        "analytics");

        System.out.println("LOG:");
        System.out.println(log.getOutput());

        System.out.println();
        System.out.println("PAYMENT PROVIDER:");
        System.out.println(paymentProvider.getOutput());

        System.out.println();
        System.out.println("ANALYTICS:");
        System.out.println(analytics.getOutput());
    }
}
```

Expected output:

```text
LOG:
{order.id=ORDER-9001, customer.email=[REDACTED]}

PAYMENT PROVIDER:
{order.id=ORDER-9001, customer.email=alice@example.com, payment.token=tok_very_secret}

ANALYTICS:
{order.id=ORDER-9001}
```

The important point is not simply whether a value is sensitive.

Satellite evaluates:

```text
what is this value?
+ where is it going?
+ why is it going there?
+ where did it come from?
+ how much do we trust it?
```

---

# Non-bypassable observability guardrail

Even an explicit `ALLOW` cannot emit confidential or privacy-sensitive values raw to logs, traces, metrics or audit sinks.

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

EgressReport report =
        new EgressProcessor(policy)
                .process(
                        EgressEnvelope.builder()
                                .put(email, "alice@example.com")
                                .build(),
                        EgressSink.LOG,
                        "debug");

System.out.println(report.getOutput());

for (EgressDecisionRecord decision : report.getDecisions()) {
    System.out.println(
            decision.getKeyName()
                    + " -> "
                    + decision.getAction()
                    + " ["
                    + decision.getReasonCode()
                    + "]");
}
```

Expected output:

```text
{}
customer.email -> DENY [OBSERVABILITY_RAW_SENSITIVE_DENIED]
```

For observability, sensitive data must be redacted, tokenized or denied — never emitted raw.

---

# When to use the advanced API

The short API is the recommended default. Use the detailed API when policy must match several dimensions at once:

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

Custom `Redactor`, `Tokenizer`, violation listeners and explicit `EgressContext` objects also remain available. Ergonomic shortcuts are additive; they do not remove the lower-level control.

# Summary

```text
SatelliteData
    dynamic application data

DataDefinition
    optional structural rules

Key<T>
    static type + classification + category

ValueMetadata
    runtime origin + trust

EgressEnvelope
    classified values inside the application

EgressPolicyEngine
    sink + purpose + metadata aware decisions

EgressReport
    approved representation only

Adapters
    policy-enforced logging, JSON and telemetry
```

The goal of Satellite Egress is not to replace logging, serialization or telemetry libraries. It creates an application-level security boundary **before data reaches those libraries or leaves the application**.
