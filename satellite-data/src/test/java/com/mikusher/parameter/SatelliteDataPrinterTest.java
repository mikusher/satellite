package com.mikusher.parameter;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SatelliteDataPrinterTest {

    @Test
    public void rendersNestedDataArraysAndNullsWithoutMutation() {
        SatelliteData nested = new SatelliteData();
        nested.put("count", 2);

        SatelliteData data = new SatelliteData();
        data.put("title", "satellite");
        data.put("nested", nested);
        data.put("items", Arrays.asList("first", "second"));
        data.put("missing", null);

        StringWriter text = new StringWriter();
        PrintWriter writer = new PrintWriter(text);
        data.prettyPrint(writer);
        writer.flush();

        String output = text.toString();

        assertTrue(output.contains("title (String) \"satellite\""));
        assertTrue(output.contains("nested (Map)"));
        assertTrue(output.contains("    count (Integer) 2"));
        assertTrue(output.contains("items (Array)"));
        assertTrue(output.contains("    (0) (String) \"first\""));
        assertTrue(output.contains("missing (NULL)"));
        assertEquals(4, data.size());
        assertEquals(2, nested.get("count"));
    }
}
