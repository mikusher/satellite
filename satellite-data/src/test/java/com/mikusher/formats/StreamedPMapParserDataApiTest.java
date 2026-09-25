package com.mikusher.formats;

import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StreamedPMapParserDataApiTest {

    @Test
    public void parsesSatelliteDataIncludingNestedValues() throws Exception {
        String xml = "<m>"
                + "<s n=\"status\">ok</s>"
                + "<m n=\"nested\"><s n=\"id\">42</s></m>"
                + "</m>";

        StreamedPMapParser parser = new StreamedPMapParser();
        SatelliteData data = parser.getData(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("ok", data.getString("status"));
        assertEquals("42", data.getData("nested").getString("id"));
    }
}
