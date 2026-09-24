package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PolicyGuardrailTest {

    @Test
    public void explicitAllowCannotEmitConfidentialValueRawToLogs() {
        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(PolicyRule.builder()
                        .key(email)
                        .sink(EgressSink.LOG)
                        .purpose("diagnostics")
                        .action(EgressAction.ALLOW)
                        .reasonCode("MISCONFIGURED_ALLOW")
                        .build())
                .add(new DefaultEgressRule())
                .build();

        PolicyDecision decision = engine.decide(
                EgressContext.of(EgressSink.LOG, "diagnostics"),
                SatelliteMap.builder().put(email, "user@example.com").build().entry(email).get());

        assertEquals(EgressAction.DENY, decision.getAction());
        assertEquals("OBSERVABILITY_RAW_SENSITIVE_DENIED", decision.getCode());
    }

    @Test
    public void explicitTokenizationStillWorksForSensitiveObservabilityData() {
        Key<String> accountId = Key.string("account.id")
                .classifiedAs(DataClassification.CONFIDENTIAL);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(PolicyRule.builder()
                        .key(accountId)
                        .sink(EgressSink.TRACE)
                        .purpose("correlation")
                        .action(EgressAction.TOKENIZE)
                        .reasonCode("TRACE_PSEUDONYM")
                        .build())
                .add(new DefaultEgressRule())
                .build();

        assertEquals(EgressAction.TOKENIZE, engine.decide(
                EgressContext.of(EgressSink.TRACE, "correlation"),
                SatelliteMap.builder().put(accountId, "123").build().entry(accountId).get())
                .getAction());
    }
}
