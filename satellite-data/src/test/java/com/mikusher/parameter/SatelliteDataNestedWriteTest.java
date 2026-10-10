package com.mikusher.parameter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class SatelliteDataNestedWriteTest {

    @Test
    public void malformedNestedPathFallsBackToLiteralName() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("items", Arrays.asList("one"));

        data.setParameter("items(nonNumeric)", "literal");
        data.setParameter("missing.member", "other");

        assertEquals("literal", data.get("items(nonNumeric)"));
        assertEquals("other", data.get("missing.member"));
        assertEquals("one", data.getParameter("items(0)"));
    }

    @Test
    public void immutableNestedCollectionFailureIsNotSilentlyConvertedToLiteralKey()
            throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("items", Collections.unmodifiableList(Arrays.asList("original")));

        try {
            data.setParameter("items(0)", "replacement");
            fail("Expected nested collection mutation failure");
        } catch (UnsupportedOperationException expected) {
            // The failure must not become an unrelated literal field.
        }

        assertFalse(data.keySet().contains("items(0)"));
        assertEquals("original", data.getParameter("items(0)"));
    }
}
