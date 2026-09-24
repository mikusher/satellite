package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EgressProcessorTest {

    @Test
    public void emitsOnlyPolicyApprovedRepresentation() {
        Key<String> status = Key.string("status").classifiedAs(DataClassification.PUBLIC);
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);
        Key<String> token = Key.string("token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        SatelliteMap map = SatelliteMap.builder()
                .put(status, "ok")
                .put(email, "user@example.com")
                .put(token, "super-secret")
                .build();

        EgressReport report = new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(map, EgressContext.of(EgressSink.LOG, "request-log"));

        assertEquals("ok", report.getOutput().get("status"));
        assertEquals("[REDACTED]", report.getOutput().get("email"));
        assertFalse(report.getOutput().containsKey("token"));
        assertTrue(report.hasViolations());
        assertFalse(report.getViolations().toString().contains("super-secret"));
    }

    @Test
    public void explicitRuleCanAuthorizeOnlySpecificPurpose() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(EgressRules.forKeyAndPurpose(
                        email,
                        EgressSink.NETWORK,
                        "account-provider",
                        EgressAction.ALLOW,
                        "CONSENTED_EMAIL_EXPORT"))
                .add(new DefaultEgressRule())
                .build();

        EgressReport allowed = new EgressProcessor(engine)
                .process(SatelliteMap.builder().put(email, "user@example.com").build(),
                        EgressContext.of(EgressSink.NETWORK, "account-provider"));

        EgressReport denied = new EgressProcessor(engine)
                .process(SatelliteMap.builder().put(email, "user@example.com").build(),
                        EgressContext.of(EgressSink.NETWORK, "analytics"));

        assertEquals("user@example.com", allowed.getOutput().get("email"));
        assertFalse(allowed.hasViolations());
        assertFalse(denied.getOutput().containsKey("email"));
        assertTrue(denied.hasViolations());
    }

    @Test(expected = IllegalArgumentException.class)
    public void broadConvenienceAllowRuleIsRejected() {
        EgressRules.forKey(
                Key.string("email"),
                EgressSink.NETWORK,
                EgressAction.ALLOW,
                "TOO_BROAD");
    }
}
