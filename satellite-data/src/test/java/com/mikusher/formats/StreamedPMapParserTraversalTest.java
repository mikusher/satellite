package com.mikusher.formats;

import com.mikusher.parameter.PMapType;
import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StreamedPMapParserTraversalTest {

    @Test
    public void findsMatchingFieldInMap() throws Exception {
        assertFindsTarget("<m><s n=\"other\">no</s><s n=\"target\">ok</s></m>", false);
    }

    @Test
    public void findsMatchingFieldInArray() throws Exception {
        assertFindsTarget("<a><s n=\"other\">no</s><s n=\"target\">ok</s></a>", true);
    }

    @Test
    public void findsNestedDescendantAfterUnmatchedSibling() throws Exception {
        assertFindsTarget(
                "<m><s n=\"other\">no</s><m n=\"group\"><s n=\"target\">ok</s></m></m>",
                false);
    }

    private static void assertFindsTarget(String xml, boolean array) throws Exception {
        XMLStreamReader reader =
                XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        try {
            reader.nextTag(); // Position on the outer container.
            SatelliteData attributes = new SatelliteData();
            attributes.put("n", "target");

            XMLStreamReader match = array
                    ? StreamedPMapParser.getXMLTreeByArray(reader, PMapType.STRING, attributes)
                    : StreamedPMapParser.getXMLTreeByMap(reader, PMapType.STRING, attributes);

            assertNotNull(match);
            assertEquals("s", match.getLocalName());
            assertEquals("target", match.getAttributeValue(null, "n"));
        } finally {
            reader.close();
        }
    }
}
