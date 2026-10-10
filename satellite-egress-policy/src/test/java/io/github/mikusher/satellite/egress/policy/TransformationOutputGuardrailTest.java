package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransformationOutputGuardrailTest {

    private static final Key<String> EMAIL = Key.string("email")
            .classifiedAs(DataClassification.CONFIDENTIAL);

    @Test
    public void customRedactorReturningRawValueIsDenied() {
        Redactor echo = entry -> (String) entry.getValue();
        EgressReport report = processorWithRedactor(echo)
                .process(envelope(), EgressSink.LOG, "debug");
        assertDenied(report, "INVALID_REDACTION_OUTPUT");
    }

    @Test
    public void missingOrUnsafeRedactionOutputIsDenied() {
        for (Redactor redactor : new Redactor[]{
                entry -> null,
                entry -> "",
                entry -> "line1\nline2",
                entry -> "  "}) {
            assertDenied(processorWithRedactor(redactor)
                    .process(envelope(), EgressSink.LOG, "debug"),
                    "INVALID_REDACTION_OUTPUT");
        }
    }

    @Test
    public void customTokenizerCannotReturnRawValue() {
        EgressPolicyEngine policy = tokenPolicy();
        Tokenizer echo = entry -> (String) entry.getValue();
        EgressReport report = new EgressProcessor(
                policy, new ConstantRedactor(), echo)
                .process(envelope(), EgressSink.STORAGE, "correlation");
        assertDenied(report, "INVALID_TOKEN_OUTPUT");
    }

    @Test
    public void nullAndControlCharacterTokensAreDenied() {
        for (Tokenizer tokenizer : new Tokenizer[]{
                entry -> null, entry -> " ",
                entry -> "token\rforged"}) {
            EgressReport report = new EgressProcessor(
                    tokenPolicy(), new ConstantRedactor(), tokenizer)
                    .process(envelope(), EgressSink.STORAGE, "correlation");
            assertDenied(report, "INVALID_TOKEN_OUTPUT");
        }
    }

    @Test
    public void validRedactionAndTokenizationStillPass() {
        EgressReport redacted = processorWithRedactor(
                entry -> "[HIDDEN]")
                .process(envelope(), EgressSink.LOG, "debug");
        assertEquals("[HIDDEN]", redacted.getOutput().get("email"));
        assertFalse(redacted.hasViolations());

        EgressReport tokenized = new EgressProcessor(
                tokenPolicy(), new ConstantRedactor(), entry -> "token:opaque")
                .process(envelope(), EgressSink.STORAGE, "correlation");
        assertEquals("token:opaque", tokenized.getOutput().get("email"));
        assertFalse(tokenized.hasViolations());
    }

    private static EgressProcessor processorWithRedactor(Redactor redactor) {
        return new EgressProcessor(
                EgressPolicyEngine.secureDefaults(),
                redactor, null);
    }

    private static EgressPolicyEngine tokenPolicy() {
        return EgressPolicyEngine.builder()
                .add(EgressRules.tokenize(
                        EMAIL, EgressSink.STORAGE, "correlation", "TOKEN_REQUIRED"))
                .withSecureDefaults()
                .build();
    }

    private static EgressEnvelope envelope() {
        return EgressEnvelope.builder().put(EMAIL, "customer@example.com").build();
    }

    private static void assertDenied(EgressReport report, String code) {
        assertTrue(report.getOutput().isEmpty());
        assertTrue(report.hasViolations());
        assertEquals(EgressAction.DENY, report.getDecisions().get(0).getAction());
        assertEquals(code, report.getDecisions().get(0).getReasonCode());
        assertFalse(report.getViolations().toString().contains("customer@example.com"));
    }
}
