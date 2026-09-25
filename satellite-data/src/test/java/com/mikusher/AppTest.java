package com.mikusher;

import com.mikusher.parameter.ParameterMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppTest {

    @Test
    public void storesAndReadsParameterValues() {
        ParameterMap map = new ParameterMap();
        map.put("project", "satellite");

        assertEquals("satellite", map.get("project"));
    }
}
