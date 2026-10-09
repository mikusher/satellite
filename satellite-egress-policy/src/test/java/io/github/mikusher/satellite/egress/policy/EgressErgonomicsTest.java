package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EgressErgonomicsTest {

    @Test
    public void secureProcessorSupportsDirectSinkAndPurpose() {
        Key<String> status = Key.string("status")
                .classifiedAs(DataClassification.PUBLIC);

        EgressReport report = EgressProcessor.secureDefaults()
                .process(
                        EgressEnvelope.builder().put(status, "ok").build(),
                        EgressSink.LOG,
                        "request-log");

        assertEquals("ok", report.getOutput().get("status"));
    }

    @Test
    public void allowConvenienceRemainsPurposeScoped() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(
                        email,
                        EgressSink.NETWORK,
                        "account-provider",
                        "ACCOUNT_EMAIL_REQUIRED"))
                .withSecureDefaults()
                .build();

        EgressProcessor processor = new EgressProcessor(policy);
        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(email, "user@example.com")
                .build();

        EgressReport allowed = processor.process(
                envelope,
                EgressSink.NETWORK,
                "account-provider");

        EgressReport denied = processor.process(
                envelope,
                EgressSink.NETWORK,
                "analytics");

        assertEquals("user@example.com", allowed.getOutput().get("email"));
        assertFalse(allowed.hasViolations());
        assertFalse(denied.getOutput().containsKey("email"));
        assertTrue(denied.hasViolations());
    }

    @Test
    public void secureDefaultsAreAppendedAfterExplicitRules() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(
                        email,
                        EgressSink.NETWORK,
                        "account-provider",
                        "ACCOUNT_EMAIL_REQUIRED"))
                .withSecureDefaults()
                .build();

        PolicyDecision decision = policy.decide(
                EgressContext.of(EgressSink.NETWORK, "account-provider"),
                EgressEnvelope.builder()
                        .put(email, "user@example.com")
                        .build()
                        .entry(email)
                        .get());

        assertEquals(EgressAction.ALLOW, decision.getAction());
        assertEquals("ACCOUNT_EMAIL_REQUIRED", decision.getCode());
    }

    @Test
    public void convenienceAllowCannotBypassObservabilityGuardrail() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(
                        email,
                        EgressSink.LOG,
                        "debug",
                        "DEBUG_EMAIL"))
                .withSecureDefaults()
                .build();

        EgressReport report = new EgressProcessor(policy)
                .process(
                        EgressEnvelope.builder()
                                .put(email, "user@example.com")
                                .build(),
                        EgressSink.LOG,
                        "debug");

        assertFalse(report.getOutput().containsKey("email"));
        assertEquals(
                "OBSERVABILITY_RAW_SENSITIVE_DENIED",
                report.getDecisions().get(0).getReasonCode());
    }

    @Test
    public void restrictiveConvenienceRulesRemainSimple() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.PUBLIC);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.redact(
                        email,
                        EgressSink.LOG,
                        "MASK_EMAIL"))
                .withSecureDefaults()
                .build();

        EgressReport report = new EgressProcessor(policy)
                .process(
                        EgressEnvelope.builder()
                                .put(email, "user@example.com")
                                .build(),
                        EgressSink.LOG,
                        "request-log");

        assertEquals("[REDACTED]", report.getOutput().get("email"));
        assertEquals("MASK_EMAIL", report.getDecisions().get(0).getReasonCode());
    }
}
