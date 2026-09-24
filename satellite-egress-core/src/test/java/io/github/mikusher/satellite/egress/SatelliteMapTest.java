package io.github.mikusher.satellite.egress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SatelliteMapTest {

    @Test
    public void storesTypedValueWithRuntimeMetadata() {
        Key<String> token = Key.string("auth.token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        SatelliteMap map = SatelliteMap.builder()
                .put(token, "secret-value",
                        ValueMetadata.of(DataOrigin.HTTP_HEADER, TrustLevel.UNTRUSTED))
                .build();

        assertEquals("secret-value", map.get(token));
        assertEquals(DataOrigin.HTTP_HEADER, map.entry(token).get().getMetadata().getOrigin());
        assertFalse(map.toString().contains("secret-value"));
        assertFalse(map.entry(token).get().toString().contains("secret-value"));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void builderRejectsTypeMismatch() {
        Key raw = Key.integer("age");
        SatelliteMap.builder().put(raw, "wrong-type");
    }
}
