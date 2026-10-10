package com.mikusher.converters;

import com.mikusher.error.IncorrectTypeException;
import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SatelliteDataConverterCleanCodeTest {

    @Test
    public void convertsStringKeyedMapWithoutUnsafeRawCast() throws Exception {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", "satellite");

        SatelliteData result = new SatelliteDataConverter().cast(source);
        assertEquals("satellite", result.getString("name"));

        source.put("name", "changed");
        assertEquals("satellite", result.getString("name"));
    }

    @Test
    public void refusesNonStringKeysInsteadOfRetainingInvalidState() throws Exception {
        Map<Object, Object> source = new HashMap<>();
        source.put(42, "invalid");

        try {
            new SatelliteDataConverter().cast(source);
            fail("Expected non-string key rejection");
        } catch (IncorrectTypeException expected) {
            // Invalid key types are rejected before entering SatelliteData.
        }
    }
}
