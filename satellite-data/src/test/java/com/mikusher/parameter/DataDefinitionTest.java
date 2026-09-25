package com.mikusher.parameter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataDefinitionTest {

    @Test
    public void constrainsSatelliteDataUsingTheNewName() throws Exception {
        DataDefinition definition = new DataDefinition("request", "Request data");
        definition.addString("id", "Request identifier");

        SatelliteData data = new SatelliteData(definition);
        data.put("id", "REQ-123");

        assertEquals("REQ-123", data.getString("id"));
    }
}
