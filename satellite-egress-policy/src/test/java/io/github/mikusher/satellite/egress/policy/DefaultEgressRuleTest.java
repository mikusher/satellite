package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultEgressRuleTest {

    @Test
    public void redactsPersonalDataInLogs() {
        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        PolicyDecision decision = EgressPolicyEngine.secureDefaults().decide(
                EgressContext.of(EgressSink.LOG, "diagnostics"),
                SatelliteMap.builder().put(email, "user@example.com").build().entry(email).get());

        assertEquals(EgressAction.REDACT, decision.getAction());
    }

    @Test
    public void deniesSecretsEverywhere() {
        Key<String> token = Key.string("auth.token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        PolicyDecision decision = EgressPolicyEngine.secureDefaults().decide(
                EgressContext.of(EgressSink.LOG, "diagnostics"),
                SatelliteMap.builder().put(token, "secret").build().entry(token).get());

        assertEquals(EgressAction.DENY, decision.getAction());
    }

    @Test
    public void publicDataIsAllowed() {
        Key<String> status = Key.string("status").classifiedAs(DataClassification.PUBLIC);

        PolicyDecision decision = EgressPolicyEngine.secureDefaults().decide(
                EgressContext.of(EgressSink.NETWORK, "public-api"),
                SatelliteMap.builder().put(status, "ok").build().entry(status).get());

        assertEquals(EgressAction.ALLOW, decision.getAction());
    }
}
