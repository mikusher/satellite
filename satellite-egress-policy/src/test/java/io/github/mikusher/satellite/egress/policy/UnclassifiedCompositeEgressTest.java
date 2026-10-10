package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnclassifiedCompositeEgressTest {

    @Test
    public void publicMapCannotCarryUnclassifiedSecretsToAnySink() {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("status", "ok");
        nested.put("password", "not-for-egress");

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, nested)
                .build();

        for (EgressSink sink : new EgressSink[]{
                EgressSink.LOG, EgressSink.TRACE,
                EgressSink.SERIALIZATION, EgressSink.NETWORK,
                EgressSink.STORAGE}) {
            EgressReport report = EgressProcessor.secureDefaults()
                    .process(envelope, sink, "regression");
            assertTrue(report.getOutput().isEmpty());
            assertTrue(report.hasViolations());
            assertEquals("UNCLASSIFIED_COMPLEX_VALUE_DENIED",
                    report.getDecisions().get(0).getReasonCode());
            assertFalse(report.getViolations().toString().contains("not-for-egress"));
        }
    }

    @Test
    public void explicitAllowCannotOverrideCompositeGuardrail() {
        Key<Object> payload = Key.of("payload", Object.class)
                .classifiedAs(DataClassification.PUBLIC);
        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, Arrays.asList("safe", "nested-secret"))
                .build();

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(
                        payload, EgressSink.NETWORK, "partner", "APPROVED_PAYLOAD"))
                .withSecureDefaults()
                .build();

        EgressReport result = new EgressProcessor(policy)
                .process(envelope, EgressSink.NETWORK, "partner");

        assertTrue(result.getOutput().isEmpty());
        assertEquals("UNCLASSIFIED_COMPLEX_VALUE_DENIED",
                result.getDecisions().get(0).getReasonCode());
    }

    @Test
    public void arraysAreNotRawEgressValues() {
        Key<byte[]> blob = Key.of("blob", byte[].class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressReport result = EgressProcessor.secureDefaults()
                .process(EgressEnvelope.builder().put(blob, new byte[]{1, 2, 3}).build(),
                        EgressSink.SERIALIZATION, "response");

        assertTrue(result.getOutput().isEmpty());
    }

    @Test
    public void explicitlyRedactedCompositeDoesNotExposeNestedValues() {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.redact(payload, EgressSink.LOG, "COMPOSITE_REDACTED"))
                .withSecureDefaults()
                .build();

        EgressReport result = new EgressProcessor(policy)
                .process(EgressEnvelope.builder()
                                .put(payload, Collections.singletonMap("secret", "sensitive"))
                                .build(),
                        EgressSink.LOG, "diagnostics");

        assertEquals("[REDACTED]", result.getOutput().get("payload"));
    }

    @Test
    public void knownImmutableScalarsStillWork() {
        Key<String> text = Key.string("status")
                .classifiedAs(DataClassification.PUBLIC);
        Key<Instant> timestamp = Key.of("time", Instant.class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressReport report = EgressProcessor.secureDefaults().process(
                EgressEnvelope.builder()
                        .put(text, "ok")
                        .put(timestamp, Instant.parse("2026-10-10T10:00:00Z"))
                        .build(),
                EgressSink.SERIALIZATION, "response");

        assertEquals("ok", report.getOutput().get("status"));
        assertEquals(Instant.parse("2026-10-10T10:00:00Z"),
                report.getOutput().get("time"));
        assertFalse(report.hasViolations());
    }
}
