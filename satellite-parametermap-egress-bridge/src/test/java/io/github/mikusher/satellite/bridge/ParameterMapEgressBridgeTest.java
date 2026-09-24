package io.github.mikusher.satellite.bridge;

import com.mikusher.parameter.ParameterMap;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.ValueMetadata;
import io.github.mikusher.satellite.egress.policy.EgressContext;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParameterMapEgressBridgeTest {

    @Test
    public void convertsOnlyExplicitlyClassifiedFields() {
        ParameterMap source = new ParameterMap();
        source.put("email", "user@example.com");

        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        BridgeResult result = ParameterMapEgressBridge.toSatelliteMap(
                source,
                Arrays.asList(email),
                ValueMetadata.of(DataOrigin.DATABASE, TrustLevel.VALIDATED));

        assertEquals("user@example.com", result.getSatelliteMap().get(email));
        assertFalse(result.hasIgnoredKeys());
    }

    @Test(expected = IllegalArgumentException.class)
    public void strictConversionRejectsUnclassifiedFields() {
        ParameterMap source = new ParameterMap();
        source.put("classified", "ok");
        source.put("forgotten-secret", "must-not-pass");

        ParameterMapEgressBridge.toSatelliteMap(
                source,
                Arrays.asList(Key.string("classified")),
                ValueMetadata.unknown());
    }

    @Test
    public void safeParameterMapContainsOnlyPolicyOutput() {
        ParameterMap source = new ParameterMap();
        source.put("email", "user@example.com");

        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        BridgeResult bridged = ParameterMapEgressBridge.toSatelliteMap(
                source, Arrays.asList(email), ValueMetadata.unknown());

        EgressReport report = new EgressProcessor(EgressPolicyEngine.secureDefaults())
                .process(bridged.getSatelliteMap(), EgressContext.of(EgressSink.LOG, "migration"));

        ParameterMap safe = ParameterMapEgressBridge.toSafeParameterMap(report);
        assertEquals("[REDACTED]", safe.get("email"));
        assertTrue(safe.keySet().contains("email"));
    }
}
