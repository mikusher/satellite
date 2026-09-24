package com.mikusher.parameter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PMapTypeTest {

    @Test
    public void resolvesTypeNamesUsingValueEquality() {
        assertEquals(PMapType.STRING, PMapType.lookup(new String("string")));
        assertEquals(PMapType.INT, PMapType.lookup(new String("int")));
        assertNull(PMapType.lookup((String) null));
    }

    @Test
    public void resolvesJavaClasses() {
        assertEquals(PMapType.STRING, PMapType.lookup(String.class));
        assertEquals(PMapType.MAP, PMapType.lookup(HashMap.class));
        assertEquals(PMapType.ARRAY, PMapType.lookup(ArrayList.class));
    }
}
