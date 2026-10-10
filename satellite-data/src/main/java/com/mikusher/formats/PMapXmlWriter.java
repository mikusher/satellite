package com.mikusher.formats;

import com.mikusher.parameter.PMapType;
import com.mikusher.formats.StreamedPMapParser.SerializationType;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.ref.WeakReference;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Encodes PMAP values into XML; parsing and resource limits remain in StreamedPMapParser.
 */
final class PMapXmlWriter {

    private static final String TAG_PARAMETER = "parameter";
    private static final int MAX_INDENT_LEVEL_CACHE = 256;
    @SuppressWarnings("unchecked")
    private static final WeakReference<String>[] INDENT_CACHE =
            new WeakReference[MAX_INDENT_LEVEL_CACHE];
    private static final IntFunction<String> INDENT_STRING_GENERATOR =
            level -> "\n" + StringUtils.repeat('\t', level);

    private PMapXmlWriter() {
    }

    private static void indentLevel(SerializationType serType, XMLStreamWriter writer, int level)
            throws XMLStreamException {

        if (!serType.ident()) {
            return;
        }

        writer.writeCharacters(getIndentLevelFromCache(level));
    }

    private static String getIndentLevelFromCache(int level) {

        if (level >= MAX_INDENT_LEVEL_CACHE) {
            return INDENT_STRING_GENERATOR.apply(level);
        }

        WeakReference<String> container = INDENT_CACHE[level];
        String value = container == null ? null : container.get();
        if (value == null) {
            value = INDENT_STRING_GENERATOR.apply(level);
            INDENT_CACHE[level] = new WeakReference<>(value);
        }

        return value;
    }

    static void writeMapContent(SerializationType serType, XMLStreamWriter writer, Map<String, ?> map,
                                          int level)
            throws XMLStreamException {

        Collection<String> keys = map.keySet();

        if (serType.ident()) {
            List<String> tmp = new ArrayList<>(map.keySet());
            Collections.sort(tmp);
            keys = tmp;
        }

        int levelBelow = level + 1;
        for (String key : keys) {
            writeValue(serType, writer, key, map.get(key), levelBelow);
        }

        indentLevel(serType, writer, level);
    }

    private static void writeList(SerializationType serType, XMLStreamWriter writer, Collection<?> list, int level)
            throws XMLStreamException {

        int levelBelow = level + 1;

        for (Object value : list) {
            writeValue(serType, writer, null, value, levelBelow);
        }
        indentLevel(serType, writer, level);
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(SerializationType serType, XMLStreamWriter writer, String key, Object value,
                                  int level)
            throws XMLStreamException {

        PMapType type = PMapType.lookup(value);
        if (type == null) {
            if (serType.ignoreUnknownTypes()) {
                // Just ignore this value because it's not supported
                return;
            }
            throw new XMLStreamException("Invalid Type - " + value.getClass().getCanonicalName());
        }

        if (serType.getVersion() == 1) {
            writer.writeStartElement(TAG_PARAMETER);
            writer.writeAttribute(StreamedPMapParser.ATT_TYPE, type.getOldPMapName());
            if (key != null) {
                writer.writeAttribute(StreamedPMapParser.ATT_NAME, key);
            }
        } else {
            indentLevel(serType, writer, level);
            writer.writeStartElement(type.getShortName());
            if (key != null) {
                writer.writeAttribute(StreamedPMapParser.ATT_NAME_SHORT, key);
            }
        }

        switch (type) {
            case STRING:
            case LONG:
            case INT:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case DECIMAL:
                writeSimpleValue(writer, value);
                break;
            case MAP:
                writeMapContent(serType, writer, (Map<String, Object>) value, level);
                break;
            case ARRAY:
                writeList(serType, writer, (Collection<?>) value, level);
                break;
            case DATE:
                writeSimpleValue(
                        writer,
                        StreamedPMapParser.DATE_FORMATTER.format(((Date) value).toInstant().atOffset(ZoneOffset.UTC)));
                break;
            case NULL:
                break;
        }
        writer.writeEndElement();

    }

    private static void writeSimpleValue(XMLStreamWriter writer, Object value) throws XMLStreamException {

        writer.writeCharacters(String.valueOf(value));
    }


}
