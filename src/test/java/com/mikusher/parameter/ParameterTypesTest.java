package com.mikusher.parameter;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParameterTypesTest {

    @Test
    public void matchesAssignableTypes() {
        assertEquals(ParameterTypes.Map, ParameterTypes.matchType(HashMap.class));
    }

    @Test
    public void validatesNullTypeSafely() throws Exception {
        assertTrue(ParameterTypes.Null.isValid(null));
        assertFalse(ParameterTypes.Null.isValid("not-null"));
    }
}
