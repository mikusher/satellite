package com.mikusher;

import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppTest {

    @Test
    public void storesAndReadsParameterValues() {
        SatelliteData map = new SatelliteData();
        map.put("project", "satellite");

        assertEquals("satellite", map.get("project"));
    }
}
