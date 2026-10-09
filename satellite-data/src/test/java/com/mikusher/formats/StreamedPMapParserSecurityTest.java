package com.mikusher.formats;

import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StreamedPMapParserSecurityTest {

    @Test
    public void parsesSimpleSatelliteData() throws Exception {
        String xml = "<?xml version=\"1.0\"?><m><s n=\"project\">satellite</s></m>";

        SatelliteData map = StreamedPMapParser.getInstance().getData(stream(xml));

        assertEquals("satellite", map.get("project"));
    }

    @Test
    public void doesNotResolveExternalEntities() throws Exception {
        String xml = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE m [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<m><s n=\"value\">&xxe;</s></m>";

        try {
            StreamedPMapParser.getInstance().getData(stream(xml));
            fail("External entity input must not be accepted");
        } catch (XMLStreamException expected) {
            // Secure parser configuration rejects entity expansion.
        }
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
