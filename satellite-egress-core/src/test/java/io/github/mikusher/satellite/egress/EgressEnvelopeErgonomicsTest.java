package io.github.mikusher.satellite.egress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EgressEnvelopeErgonomicsTest {

    @Test
    public void acceptsOriginAndTrustWithoutManualValueMetadata() {
        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(
                        email,
                        "user@example.com",
                        DataOrigin.DATABASE,
                        TrustLevel.VALIDATED)
                .build();

        SatelliteEntry<String> entry = envelope.entry(email).get();

        assertEquals("user@example.com", entry.getValue());
        assertEquals(DataOrigin.DATABASE, entry.getMetadata().getOrigin());
        assertEquals(TrustLevel.VALIDATED, entry.getMetadata().getTrustLevel());
    }
}
