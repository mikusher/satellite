package com.mikusher.formats;

import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamedPMapParserOutputTest {

    @Test
    public void doesNotCloseCallerOwnedOutputStream() throws Exception {
        TrackingOutputStream output = new TrackingOutputStream();

        StreamedPMapParser.getInstance().PMAPtoOutputStream(
                Collections.<String, Object>singletonMap("status", "ok"),
                StreamedPMapParser.SerializationType.PMAP2,
                output);

        assertFalse(output.closed);
        output.write('!');
        assertTrue(output.size() > 0);
    }

    @Test(expected = XMLStreamException.class)
    public void rejectsUnknownTreeTypeWithXmlException() throws Exception {
        javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
        javax.xml.stream.XMLStreamReader reader =
                factory.createXMLStreamReader(new java.io.StringReader("<unknown/>"));
        try {
            reader.nextTag();
            StreamedPMapParser.getInstance().parseTree(reader);
        } finally {
            reader.close();
        }
    }

    private static final class TrackingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private boolean closed;

        @Override
        public void write(int value) throws IOException {
            if (closed) {
                throw new IOException("stream is closed");
            }
            delegate.write(value);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            if (closed) {
                throw new IOException("stream is closed");
            }
            delegate.write(buffer, offset, length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }

        int size() {
            return delegate.size();
        }
    }
}
