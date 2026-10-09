package io.github.mikusher.satellite.bridge;

import com.mikusher.parameter.SatelliteData;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.ValueMetadata;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SatelliteDataEgressBridgeTest {

    @Test
    public void convertsClassifiedSatelliteDataToEnvelope() {
        SatelliteData data = new SatelliteData();
        data.put("email", "user@example.com");

        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        DataBridgeResult result = SatelliteDataEgressBridge.toEnvelope(
                data,
                Arrays.asList(email),
                ValueMetadata.of(
                        DataOrigin.DATABASE,
                        TrustLevel.VALIDATED));

        EgressEnvelope envelope = result.getEnvelope();

        assertEquals("user@example.com", envelope.get(email));
        assertFalse(result.hasIgnoredKeys());
    }

    @Test(expected = IllegalArgumentException.class)
    public void strictModeRejectsUnclassifiedData() {
        SatelliteData data = new SatelliteData();
        data.put("known", "ok");
        data.put("unknown", "secret");

        SatelliteDataEgressBridge.toEnvelope(
                data,
                Arrays.asList(Key.string("known")),
                ValueMetadata.unknown());
    }
}
