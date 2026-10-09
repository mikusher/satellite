package io.github.mikusher.satellite.bridge;

import com.mikusher.parameter.SatelliteData;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import io.github.mikusher.satellite.egress.TrustLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SatelliteDataEgressBridgeErgonomicsTest {

    @Test
    public void schemaCanBeUsedAsTheKeyRegistry() {
        Key<String> userId = Key.string("user.id")
                .classifiedAs(DataClassification.PUBLIC);

        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        SatelliteSchema schema = SatelliteSchema.builder("User")
                .required(userId)
                .optional(email)
                .build();

        SatelliteData data = new SatelliteData();
        data.put("user.id", "user-123");
        data.put("user.email", "user@example.com");

        DataBridgeResult result = SatelliteDataEgressBridge.toEnvelope(
                data,
                schema,
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED);

        EgressEnvelope envelope = result.getEnvelope();

        assertEquals("user-123", envelope.get(userId));
        assertEquals("user@example.com", envelope.get(email));
        assertEquals(DataOrigin.APPLICATION,
                envelope.entry(email).get().getMetadata().getOrigin());
        assertEquals(TrustLevel.VALIDATED,
                envelope.entry(email).get().getMetadata().getTrustLevel());
        assertFalse(result.hasIgnoredKeys());
    }

    @Test(expected = IllegalArgumentException.class)
    public void schemaOverloadRejectsMissingRequiredFields() {
        Key<String> required = Key.string("required")
                .classifiedAs(DataClassification.PUBLIC);

        SatelliteSchema schema = SatelliteSchema.builder("Required")
                .required(required)
                .build();

        SatelliteData data = new SatelliteData();

        SatelliteDataEgressBridge.toEnvelope(
                data,
                schema,
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void schemaOverloadRemainsStrictForUnknownFields() {
        Key<String> known = Key.string("known")
                .classifiedAs(DataClassification.PUBLIC);

        SatelliteSchema schema = SatelliteSchema.builder("Strict")
                .optional(known)
                .build();

        SatelliteData data = new SatelliteData();
        data.put("known", "ok");
        data.put("unknown", "secret");

        SatelliteDataEgressBridge.toEnvelope(
                data,
                schema,
                DataOrigin.APPLICATION,
                TrustLevel.VALIDATED);
    }
}
