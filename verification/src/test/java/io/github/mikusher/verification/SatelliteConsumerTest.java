package io.github.mikusher.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikusher.parameter.SatelliteData;
import io.github.mikusher.satellite.bridge.DataBridgeResult;
import io.github.mikusher.satellite.bridge.SatelliteDataEgressBridge;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.jackson.JacksonEgressSerializer;
import io.github.mikusher.satellite.egress.observability.SafeLogEvent;
import io.github.mikusher.satellite.egress.observability.Slf4jEgressLogger;
import io.github.mikusher.satellite.egress.opentelemetry.OpenTelemetrySpanAdapter;
import io.github.mikusher.satellite.egress.policy.ConstantRedactor;
import io.github.mikusher.satellite.egress.policy.EgressAction;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressRules;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import io.opentelemetry.api.common.AttributeKey;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Consumer-side tests: all seven Satellite artifacts are resolved outside the reactor.
 * No live network call, external collector or production credentials are used.
 */
public class SatelliteConsumerTest {

    private static final Key<String> ORDER_ID =
            Key.string("order.id").classifiedAs(DataClassification.PUBLIC);
    private static final Key<String> EMAIL =
            Key.string("customer.email")
                    .classifiedAs(DataClassification.CONFIDENTIAL)
                    .category(DataCategory.PERSONAL_DATA);
    private static final Key<String> TOKEN =
            Key.string("payment.token")
                    .classifiedAs(DataClassification.RESTRICTED)
                    .category(DataCategory.CREDENTIAL);

    @Test
    public void paymentDataTravelsThroughBridgePolicyAndAllAdapters() throws Exception {
        SatelliteData source = new SatelliteData();
        source.put("order.id", "ORDER-9001");
        source.put("customer.email", "alice@example.com");
        source.put("payment.token", "tok_private");

        SatelliteSchema schema = SatelliteSchema.builder("Payment")
                .required(ORDER_ID).optional(EMAIL).optional(TOKEN).build();
        DataBridgeResult bridged = SatelliteDataEgressBridge.toEnvelope(
                source, schema, DataOrigin.APPLICATION, TrustLevel.VALIDATED);
        assertFalse(bridged.hasIgnoredKeys());

        EgressEnvelope envelope = bridged.getEnvelope();
        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(EMAIL, EgressSink.NETWORK,
                        "payment-provider", "PAYMENT_EMAIL_REQUIRED"))
                .add(EgressRules.allow(TOKEN, EgressSink.NETWORK,
                        "payment-provider", "PAYMENT_TOKEN_REQUIRED"))
                .withSecureDefaults().build();
        EgressProcessor processor = new EgressProcessor(policy);

        EgressReport payment = processor.process(
                envelope, EgressSink.NETWORK, "payment-provider");
        assertEquals("ORDER-9001", payment.getOutput().get("order.id"));
        assertEquals("alice@example.com", payment.getOutput().get("customer.email"));
        assertEquals("tok_private", payment.getOutput().get("payment.token"));

        EgressReport analytics = processor.process(
                envelope, EgressSink.NETWORK, "analytics");
        assertEquals(Collections.singletonMap("order.id", "ORDER-9001"),
                analytics.getOutput());

        EgressReport log = EgressProcessor.secureDefaults().process(
                envelope, EgressSink.LOG, "payment-log");
        assertEquals("ORDER-9001", log.getOutput().get("order.id"));
        assertEquals("[REDACTED]", log.getOutput().get("customer.email"));
        assertFalse(log.getOutput().containsKey("payment.token"));

        String json = JacksonEgressSerializer.secure(new ObjectMapper())
                .toJson(envelope, "api-response");
        assertTrue(json.contains("ORDER-9001"));
        assertTrue(json.contains("[REDACTED]"));
        assertFalse(json.contains("alice@example.com"));
        assertFalse(json.contains("tok_private"));

        SafeLogEvent event = Slf4jEgressLogger
                .secure(LoggerFactory.getLogger(SatelliteConsumerTest.class))
                .prepare("payment completed", envelope, "payment-log");
        assertTrue(event.getLine().contains("ORDER-9001"));
        assertTrue(event.getLine().contains("[REDACTED]"));
        assertFalse(event.getLine().contains("alice@example.com"));
        assertFalse(event.getLine().contains("tok_private"));

        io.opentelemetry.api.common.Attributes attributes =
                OpenTelemetrySpanAdapter.secure()
                        .prepareAttributes(envelope, "payment-trace")
                        .getAttributes();
        assertEquals("ORDER-9001",
                attributes.get(AttributeKey.stringKey("order.id")));
        assertEquals("[REDACTED]",
                attributes.get(AttributeKey.stringKey("customer.email")));
        assertNull(attributes.get(AttributeKey.stringKey("payment.token")));
    }

    @Test
    public void strictBridgeRejectsUnknownKeysAndMissingRequiredValues() {
        SatelliteSchema schema = SatelliteSchema.builder("Strict")
                .required(ORDER_ID).build();

        SatelliteData unknown = new SatelliteData();
        unknown.put("order.id", "ORDER-1");
        unknown.put("secret", "not-classified");
        expectBridgeFailure(unknown, schema);

        SatelliteData missingRequired = new SatelliteData();
        expectBridgeFailure(missingRequired, schema);
    }

    @Test
    public void publicCompositeCannotBeExportedByAnyAdapter() throws Exception {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);
        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, Collections.singletonMap("secret", "nested-private-value"))
                .build();

        EgressReport report = EgressProcessor.secureDefaults()
                .process(envelope, EgressSink.LOG, "diagnostics");
        assertTrue(report.getOutput().isEmpty());
        assertEquals("UNCLASSIFIED_COMPLEX_VALUE_DENIED",
                report.getDecisions().get(0).getReasonCode());

        String json = JacksonEgressSerializer.secure(new ObjectMapper())
                .toJson(envelope, "api-response");
        assertEquals("{}", json);

        String log = Slf4jEgressLogger
                .secure(LoggerFactory.getLogger(SatelliteConsumerTest.class))
                .prepare("done", envelope, "diagnostics").getLine();
        assertFalse(log.contains("nested-private-value"));

        assertNull(OpenTelemetrySpanAdapter.secure()
                .prepareAttributes(envelope, "diagnostics").getAttributes()
                .get(AttributeKey.stringKey("payload")));
    }

    @Test
    public void rawEchoTransformationsFailClosed() {
        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(EMAIL, "alice@example.com").build();

        EgressReport redacted = new EgressProcessor(
                EgressPolicyEngine.secureDefaults(),
                entry -> (String) entry.getValue(),
                null).process(envelope, EgressSink.LOG, "debug");
        assertTrue(redacted.getOutput().isEmpty());
        assertEquals("INVALID_REDACTION_OUTPUT",
                redacted.getDecisions().get(0).getReasonCode());
        assertEquals(EgressAction.DENY, redacted.getDecisions().get(0).getAction());

        EgressPolicyEngine tokenPolicy = EgressPolicyEngine.builder()
                .add(EgressRules.tokenize(EMAIL, EgressSink.STORAGE,
                        "analytics", "TOKEN_REQUIRED"))
                .withSecureDefaults().build();
        EgressReport tokenized = new EgressProcessor(
                tokenPolicy, new ConstantRedactor(),
                entry -> (String) entry.getValue())
                .process(envelope, EgressSink.STORAGE, "analytics");
        assertTrue(tokenized.getOutput().isEmpty());
        assertEquals("INVALID_TOKEN_OUTPUT",
                tokenized.getDecisions().get(0).getReasonCode());
    }

    private static void expectBridgeFailure(SatelliteData source, SatelliteSchema schema) {
        try {
            SatelliteDataEgressBridge.toEnvelope(
                    source, schema, DataOrigin.APPLICATION, TrustLevel.VALIDATED);
            fail("Strict bridge should reject unclassified or missing data");
        } catch (IllegalArgumentException expected) {
            // Explicit fail-closed behavior, not an integration crash.
        }
    }
}
