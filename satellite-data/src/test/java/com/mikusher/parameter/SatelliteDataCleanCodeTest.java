package com.mikusher.parameter;

import com.mikusher.error.UnknownParameterException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class SatelliteDataCleanCodeTest {

    @Test
    public void dataDefaultUsesTheCorrectTypeAndName() throws Exception {
        SatelliteData fallback = new SatelliteData();
        fallback.put("id", "fallback");

        SatelliteData data = new SatelliteData();
        assertSame(fallback, data.getDataOrDefault("nested", fallback));

        SatelliteData nested = new SatelliteData();
        nested.put("id", "actual");
        data.put("nested", nested);
        assertSame(nested, data.getDataOrDefault("nested", fallback));

        data.put("rate", 2.5d);
        assertEquals(2.5d, data.getDoubleOrDefault("rate", 0d), 0d);
    }

    @Test
    public void nestedReadsHandleMapsAndLists() throws Exception {
        SatelliteData child = new SatelliteData();
        child.put("id", "123");

        SatelliteData data = new SatelliteData();
        data.put("child", child);
        data.put("items", Arrays.asList(child));

        assertEquals("123", data.getString("child.id"));
        assertEquals("123", data.getString("items(0).id"));
    }

    @Test
    public void invalidNestedPathsAreNotAccidentallyAccepted() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("items", Arrays.asList("first"));

        assertUnknown(data, "items(no-number)");
        assertUnknown(data, "items(9)");
        assertUnknown(data, "items(0).field");
        assertUnknown(data, "missing.field");
    }

    @Test
    public void typedMapConstructorKeepsDocumentedBackingMapBehavior() {
        Map<String, Object> backing = new HashMap<>();
        backing.put("name", "initial");

        SatelliteData data = new SatelliteData(backing);
        backing.put("name", "updated");

        assertEquals("updated", data.get("name"));
        data.put("second", 2);
        assertEquals(2, backing.get("second"));
    }

    @Test
    public void clonePreservesValuesWithSupportedReflection() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("name", "satellite");
        SatelliteData cloned = data.clone();

        assertEquals("satellite", cloned.getString("name"));
        cloned.put("name", "other");
        assertEquals("satellite", data.getString("name"));
    }

    private static void assertUnknown(SatelliteData data, String path) throws Exception {
        try {
            data.getParameter(path);
            fail("Expected an unknown path: " + path);
        } catch (UnknownParameterException expected) {
            // Invalid paths use the same not-found contract as before.
        }
    }
}
