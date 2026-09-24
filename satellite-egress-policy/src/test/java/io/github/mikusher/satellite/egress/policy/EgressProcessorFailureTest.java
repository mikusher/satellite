package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EgressProcessorFailureTest {

    @Test
    public void tokenizerFailureIsConvertedToDenyWithoutValueLeak() {
        Key<String> id = Key.string("customer.id")
                .classifiedAs(DataClassification.CONFIDENTIAL);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(PolicyRule.builder()
                        .key(id)
                        .sink(EgressSink.STORAGE)
                        .purpose("analytics")
                        .action(EgressAction.TOKENIZE)
                        .reasonCode("PSEUDONYMIZE")
                        .build())
                .add(new DefaultEgressRule())
                .build();

        Tokenizer brokenTokenizer = entry -> {
            throw new IllegalStateException("secret-value-must-not-appear");
        };

        EgressReport report = new EgressProcessor(
                engine,
                new ConstantRedactor(),
                brokenTokenizer)
                .process(
                        SatelliteMap.builder().put(id, "secret-value").build(),
                        EgressContext.of(EgressSink.STORAGE, "analytics"));

        assertFalse(report.getOutput().containsKey("customer.id"));
        assertTrue(report.hasViolations());
        assertEquals("TOKENIZATION_FAILED", report.getViolations().get(0).getReasonCode());
        assertFalse(report.getViolations().toString().contains("secret-value"));
    }

    @Test
    public void redactorFailureIsConvertedToDeny() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL);

        Redactor brokenRedactor = entry -> {
            throw new IllegalStateException("failed");
        };

        EgressReport report = new EgressProcessor(
                EgressPolicyEngine.secureDefaults(),
                brokenRedactor,
                null)
                .process(
                        SatelliteMap.builder().put(email, "user@example.com").build(),
                        EgressContext.of(EgressSink.LOG, "diagnostics"));

        assertFalse(report.getOutput().containsKey("email"));
        assertTrue(report.hasViolations());
        assertEquals("REDACTION_FAILED", report.getViolations().get(0).getReasonCode());
    }
}
