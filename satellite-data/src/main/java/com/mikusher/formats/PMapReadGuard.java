package com.mikusher.formats;

import javax.xml.stream.XMLStreamException;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

/**
 * Tracks resource consumption for one PMAP parse.
 *
 * <p>A fresh instance is created for every parse; no counters or limits are
 * shared across parser calls or threads.</p>
 */
final class PMapReadGuard {
    private final PMapParserLimits limits;
    private int entries;

    PMapReadGuard(PMapParserLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    void checkDepth(int depth) throws XMLStreamException {
        if (depth > limits.getMaxDepth()) {
            throw new XMLStreamException("PMAP nesting depth limit exceeded");
        }
    }

    void consumeEntry() throws XMLStreamException {
        entries++;
        if (entries > limits.getMaxEntries()) {
            throw new XMLStreamException("PMAP entry limit exceeded");
        }
    }

    void checkText(String value) throws XMLStreamException {
        if (value != null && value.length() > limits.getMaxTextLength()) {
            throw new XMLStreamException("PMAP text length limit exceeded");
        }
    }

    static InputStream limitBytes(InputStream input, long maxBytes) {
        return new LimitedInputStream(input, maxBytes);
    }

    static Reader limitCharacters(Reader input, long maxCharacters) {
        return new LimitedReader(input, maxCharacters);
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long count;

        private LimitedInputStream(InputStream input, long maxBytes) {
            super(Objects.requireNonNull(input, "input"));
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count++;
                checkLimit();
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count += read;
                checkLimit();
            }
            return read;
        }

        private void checkLimit() throws IOException {
            if (count > maxBytes) {
                throw new IOException("PMAP input size limit exceeded");
            }
        }
    }

    private static final class LimitedReader extends FilterReader {
        private final long maxCharacters;
        private long count;

        private LimitedReader(Reader reader, long maxCharacters) {
            super(Objects.requireNonNull(reader, "reader"));
            this.maxCharacters = maxCharacters;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count++;
                checkLimit();
            }
            return value;
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count += read;
                checkLimit();
            }
            return read;
        }

        private void checkLimit() throws IOException {
            if (count > maxCharacters) {
                throw new IOException("PMAP input size limit exceeded");
            }
        }
    }
}
