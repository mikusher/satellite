package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenizationFailureTest {

    @Test
    public void missingTokenizerFailsClosed() {
        Key<String> id = Key.string("customer.id")
                .classifiedAs(DataClassification.CONFIDENTIAL);

        EgressPolicyEngine engine = EgressPolicyEngine.builder()
                .add(EgressRules.forKeyAndPurpose(
                        id,
                        EgressSink.STORAGE,
                        "analytics",
                        EgressAction.TOKENIZE,
                        "PSEUDONYMIZE"))
                .add(new DefaultEgressRule())
                .build();

        EgressReport report = new EgressProcessor(engine)
                .process(SatelliteMap.builder().put(id, "123").build(),
                        EgressContext.of(EgressSink.STORAGE, "analytics"));

        assertFalse(report.getOutput().containsKey("customer.id"));
        assertTrue(report.hasViolations());
    }
}
