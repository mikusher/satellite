package com.mikusher.formats;

import com.mikusher.parameter.SatelliteData;
import org.junit.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PMapXmlWriterTest {

    @Test
    public void writesAndReadsNestedValuesInBothFormats() throws Exception {
        SatelliteData child = new SatelliteData();
        child.put("id", "nested");

        SatelliteData source = new SatelliteData();
        source.put("status", "<ready>&");
        source.put("child", child);
        source.put("items", Arrays.asList("first", 3));
        source.put("date", Date.from(Instant.parse("2024-03-04T05:06:07Z")));

        StreamedPMapParser parser = new StreamedPMapParser(PMapParserLimits.defaults());
        for (StreamedPMapParser.SerializationType format :
                Arrays.asList(
                        StreamedPMapParser.SerializationType.PMAP1,
                        StreamedPMapParser.SerializationType.PMAP2,
                        StreamedPMapParser.SerializationType.PMAP2_WITH_FORMATTING)) {
            byte[] written = parser.PMAPtoByteArray(source, format);
            String xml = new String(written, StandardCharsets.UTF_8);

            assertTrue("XML should escape content", xml.contains("&lt;ready&gt;&amp;"));
            assertTrue("The date format must be UTC", xml.contains("20240304050607"));

            SatelliteData parsed = parser.byteArrayToData(format, written);
            assertEquals("<ready>&", parsed.getString("status"));
            assertEquals("nested", parsed.getData("child").getString("id"));
            assertEquals(Arrays.asList("first", 3), parsed.getArray("items"));
            assertEquals(source.getDate("date"), parsed.getDate("date"));
        }
    }

    @Test
    public void formattedXmlKeepsDeterministicKeyOrdering() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("zeta", "last");
        data.put("alpha", "first");

        String xml = new StreamedPMapParser(PMapParserLimits.defaults()).toXMLString(
                data, StreamedPMapParser.SerializationType.PMAP2_WITH_FORMATTING);

        assertTrue(xml.contains("\n"));
        assertTrue(xml.indexOf("n=\"alpha\"") < xml.indexOf("n=\"zeta\""));
    }

    @Test
    public void unknownTypesAreHandledAccordingToFormat() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("status", "ok");
        data.put("unknown", new Object());

        StreamedPMapParser parser = new StreamedPMapParser(PMapParserLimits.defaults());
        try {
            parser.toXMLString(data, StreamedPMapParser.SerializationType.PMAP2);
            fail("Unknown objects must be rejected in strict writing mode");
        } catch (XMLStreamException expected) {
            // Strict behavior is unchanged after extracting the writer.
        }

        String xml = parser.toXMLString(
                data, StreamedPMapParser.SerializationType.PMAP2_NO_UNKNOWN);
        assertTrue(xml.contains("status"));
        assertFalse(xml.contains("unknown"));
    }

    @Test
    public void legacyWriterEntryPointStaysUsableWithCallerWriter() throws Exception {
        StringWriter buffer = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(buffer);
        StreamedPMapParser parser = new StreamedPMapParser(PMapParserLimits.defaults());

        writer.writeStartElement("m");
        SatelliteData data = new SatelliteData();
        data.put("status", "ok");
        parser.XMLWriterToMapWithoutRoot(
                StreamedPMapParser.SerializationType.PMAP2, writer, data);
        writer.writeEndElement();
        writer.flush();

        assertTrue(buffer.toString().contains("<s n=\"status\">ok</s>"));
        writer.close();
    }
}
