package com.mikusher.parameter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataDefinitionCleanCodeTest {

    @Test
    public void copyPreservesNameDescriptionAndDefinitions() {
        DataDefinition original =
                new DataDefinition("Request", "Incoming request data");
        original.addString("requestId", "Identifier");

        DataDefinition copied = new DataDefinition(original);

        assertEquals("Request", copied.getName());
        assertEquals("Incoming request data", copied.getDescription());
        assertTrue(copied.containsParameter("requestId"));
    }
}
