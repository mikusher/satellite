package io.github.mikusher.satellite.egress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EgressEnvelopeTest {

    @Test
    public void storesTypedValuesAndRuntimeMetadata() {
        Key<String> token = Key.string("auth.token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(
                        token,
                        "secret-value",
                        ValueMetadata.of(
                                DataOrigin.HTTP_HEADER,
                                TrustLevel.UNTRUSTED))
                .build();

        assertEquals("secret-value", envelope.get(token));
        assertEquals(
                DataOrigin.HTTP_HEADER,
                envelope.entry(token).get().getMetadata().getOrigin());
        assertFalse(envelope.toString().contains("secret-value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsClassificationCollisionByExternalName() {
        Key<String> secret = Key.string("value")
                .classifiedAs(DataClassification.RESTRICTED);
        Key<String> publicAlias = Key.string("value")
                .classifiedAs(DataClassification.PUBLIC);

        EgressEnvelope.builder()
                .put(secret, "secret")
                .put(publicAlias, "downgraded");
    }
}
