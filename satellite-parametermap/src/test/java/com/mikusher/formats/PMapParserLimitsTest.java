package com.mikusher.formats;

import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class PMapParserLimitsTest {

    @Test(expected = XMLStreamException.class)
    public void rejectsExcessiveCollectionSize() throws Exception {
        StreamedPMapParser parser = new StreamedPMapParser(
                PMapParserLimits.builder().maxCollectionSize(1).build());

        parser.getMap(stream("<m><s n=\"a\">1</s><s n=\"b\">2</s></m>"));
    }

    @Test(expected = XMLStreamException.class)
    public void rejectsExcessiveNestingDepth() throws Exception {
        StreamedPMapParser parser = new StreamedPMapParser(
                PMapParserLimits.builder().maxDepth(2).build());

        parser.getMap(stream("<m><m n=\"a\"><m n=\"b\"><s n=\"v\">x</s></m></m></m>"));
    }

    @Test(expected = XMLStreamException.class)
    public void rejectsOversizedText() throws Exception {
        StreamedPMapParser parser = new StreamedPMapParser(
                PMapParserLimits.builder().maxTextLength(3).build());

        parser.getMap(stream("<m><s n=\"v\">abcd</s></m>"));
    }

    @Test(expected = XMLStreamException.class)
    public void rejectsOversizedInput() throws Exception {
        StreamedPMapParser parser = new StreamedPMapParser(
                PMapParserLimits.builder().maxInputBytes(16).build());

        parser.getMap(stream("<m><s n=\"value\">this-is-too-large</s></m>"));
    }

    @Test(expected = XMLStreamException.class)
    public void rejectsOversizedReaderInputByCharacterCount() throws Exception {
        StreamedPMapParser parser = new StreamedPMapParser(
                PMapParserLimits.builder().maxInputCharacters(16).build());

        parser.getMap(new StringReader("<m><s n=\"value\">this-is-too-large</s></m>"));
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
