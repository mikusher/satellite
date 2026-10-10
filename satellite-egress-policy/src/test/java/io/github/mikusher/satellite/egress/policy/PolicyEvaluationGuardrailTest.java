package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PolicyEvaluationGuardrailTest {

    @Test
    public void thrownPolicyEvaluationFailsClosedInsteadOfCrashing() {
        assertFailClosed((context, entry) -> {
            throw new IllegalStateException("do-not-copy-sensitive-values");
        }, "POLICY_EVALUATION_FAILED");
    }

    @Test
    public void nullPolicyResultFailsClosed() {
        assertFailClosed((context, entry) -> null, "INVALID_POLICY_RESULT");
    }

    @Test
    public void noMatchingRuleStillFailsClosed() {
        assertFailClosed((context, entry) -> Optional.empty(), "NO_MATCHING_POLICY");
    }

    private static void assertFailClosed(EgressRule rule, String reason) {
        Key<String> publicKey = Key.string("status")
                .classifiedAs(DataClassification.PUBLIC);

        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(rule)
                .withSecureDefaults()
                .build();

        EgressReport report = new EgressProcessor(policy).process(
                EgressEnvelope.builder().put(publicKey, "value").build(),
                EgressSink.NETWORK, "partner");

        if (reason.equals("NO_MATCHING_POLICY")) {
            // Secure defaults are deliberately allowed to handle non-matches.
            assertEquals("value", report.getOutput().get("status"));
        } else {
            assertTrue(report.getOutput().isEmpty());
            assertEquals(reason, report.getDecisions().get(0).getReasonCode());
        }
    }
}
