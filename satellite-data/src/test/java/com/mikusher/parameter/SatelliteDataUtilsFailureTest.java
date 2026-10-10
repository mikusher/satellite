package com.mikusher.parameter;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class SatelliteDataUtilsFailureTest {

    @Test
    public void invalidTypedValuesStillReturnExplicitFallbacks() {
        SatelliteData data = new SatelliteData();
        data.put("retry", "not-a-number");

        assertEquals(Integer.valueOf(3),
                SatelliteDataUtils.getInt("retry", data, 3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void writeFailuresAreNotSilentlyDiscarded() {
        SatelliteData data = new SatelliteData(
                Collections.unmodifiableMap(new HashMap<String, Object>()));

        SatelliteDataUtils.setString("status", "ok", data);
    }
}
