package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.ValueMetadata;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PolicyRuleTest {

    @Test
    public void explicitAllowIsLimitedToDeclaredPurpose() {
        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(PolicyRule.builder()
                        .key(email)
                        .sink(EgressSink.NETWORK)
                        .purpose("account-provider")
                        .action(EgressAction.ALLOW)
                        .reasonCode("ACCOUNT_EMAIL_REQUIRED")
                        .build())
                .add(new DefaultEgressRule())
                .build();

        assertEquals(EgressAction.ALLOW, engine.decide(
                EgressContext.of(EgressSink.NETWORK, "account-provider"),
                SatelliteMap.builder().put(email, "user@example.com").build().entry(email).get())
                .getAction());

        assertEquals(EgressAction.DENY, engine.decide(
                EgressContext.of(EgressSink.NETWORK, "analytics"),
                SatelliteMap.builder().put(email, "user@example.com").build().entry(email).get())
                .getAction());
    }

    @Test
    public void ruleCanMatchRuntimeOriginAndTrust() {
        Key<String> publicInput = Key.string("search.term")
                .classifiedAs(DataClassification.PUBLIC);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(PolicyRule.builder()
                        .origin(DataOrigin.USER_INPUT)
                        .trustLevel(TrustLevel.UNTRUSTED)
                        .sink(EgressSink.LOG)
                        .action(EgressAction.REDACT)
                        .reasonCode("UNTRUSTED_INPUT_REDACTED")
                        .build())
                .add(new DefaultEgressRule())
                .build();

        assertEquals(EgressAction.REDACT, engine.decide(
                EgressContext.of(EgressSink.LOG, "diagnostics"),
                SatelliteMap.builder()
                        .put(publicInput, "attacker-input",
                                ValueMetadata.of(DataOrigin.USER_INPUT, TrustLevel.UNTRUSTED))
                        .build()
                        .entry(publicInput).get())
                .getAction());
    }

    @Test(expected = IllegalArgumentException.class)
    public void contextRejectsControlCharactersInPurpose() {
        EgressContext.of(EgressSink.LOG, "unsafe\npurpose");
    }
}
