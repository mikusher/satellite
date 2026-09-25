package com.mikusher.parameter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SatelliteDataTest {

    @Test
    public void providesTheParameterMapBehaviorUnderTheNewName() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("caseNumber", "C12.12343");
        data.put("retry", 3);

        assertEquals("C12.12343", data.getString("caseNumber"));
        assertEquals(3, data.getInt("retry"));
    }

    @Test
    public void cloneKeepsTheNewType() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("value", "satellite");

        SatelliteData copy = data.clone();

        assertTrue(copy instanceof SatelliteData);
        assertEquals("satellite", copy.getString("value"));
    }
}
