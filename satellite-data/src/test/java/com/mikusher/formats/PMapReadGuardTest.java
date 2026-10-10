package com.mikusher.formats;

import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PMapReadGuardTest {

    @Test
    public void acceptsExactlyTheByteLimit() throws IOException {
        byte[] payload = "abc".getBytes(StandardCharsets.UTF_8);
        InputStream input = PMapReadGuard.limitBytes(
                new ByteArrayInputStream(payload), payload.length);

        byte[] output = new byte[3];
        assertEquals(3, input.read(output, 0, output.length));
        assertEquals(-1, input.read());
    }

    @Test
    public void rejectsBulkReadsBeyondTheByteLimit() throws IOException {
        InputStream input = PMapReadGuard.limitBytes(
                new ByteArrayInputStream("abcd".getBytes(StandardCharsets.UTF_8)), 3);

        try {
            input.read(new byte[4], 0, 4);
            fail("Expected a byte budget violation");
        } catch (IOException expected) {
            assertEquals("PMAP input size limit exceeded", expected.getMessage());
        }
    }

    @Test
    public void rejectsSingleByteReadsBeyondTheLimit() throws IOException {
        InputStream input = PMapReadGuard.limitBytes(
                new ByteArrayInputStream("abcd".getBytes(StandardCharsets.UTF_8)), 3);

        for (int i = 0; i < 3; i++) {
            input.read();
        }
        try {
            input.read();
            fail("Expected a byte budget violation");
        } catch (IOException expected) {
            assertEquals("PMAP input size limit exceeded", expected.getMessage());
        }
    }

    @Test
    public void enforcesCharacterLimit() throws IOException {
        Reader reader = PMapReadGuard.limitCharacters(new StringReader("abcd"), 3);
        char[] output = new char[3];
        assertEquals(3, reader.read(output, 0, 3));

        try {
            reader.read();
            fail("Expected a character budget violation");
        } catch (IOException expected) {
            assertEquals("PMAP input size limit exceeded", expected.getMessage());
        }
    }

    @Test
    public void checksDepthEntriesAndTextIndependently() throws XMLStreamException {
        PMapReadGuard guard = new PMapReadGuard(
                PMapParserLimits.builder()
                        .maxDepth(2)
                        .maxEntries(2)
                        .maxTextLength(3)
                        .build());

        guard.checkDepth(2);
        guard.consumeEntry();
        guard.consumeEntry();
        guard.checkText("abc");
        guard.checkText(null);

        expectXmlLimit(() -> guard.checkDepth(3), "PMAP nesting depth limit exceeded");
        expectXmlLimit(guard::consumeEntry, "PMAP entry limit exceeded");
        expectXmlLimit(() -> guard.checkText("abcd"), "PMAP text length limit exceeded");
    }

    private static void expectXmlLimit(XmlAction action, String message) {
        try {
            action.run();
            fail("Expected an XML resource budget violation");
        } catch (XMLStreamException expected) {
            assertEquals(message, expected.getMessage());
        }
    }

    private interface XmlAction {
        void run() throws XMLStreamException;
    }
}
