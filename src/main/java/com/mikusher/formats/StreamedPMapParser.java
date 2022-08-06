package com.mikusher.formats;


import com.google.common.collect.Maps;
import com.mikusher.error.CoreError;
import com.mikusher.error.CoreException;
import com.mikusher.error.SatelliteException;
import com.mikusher.parameter.PMapType;
import com.mikusher.parameter.ParameterMap;
import com.mikusher.parameter.ParameterMapUtils;
import com.mikusher.utils.PMapReadPlugin;
import com.mikusher.utils.StaxUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.IntFunction;

public class StreamedPMapParser {


    public static final String ATT_NAME_SHORT = "n";
    static final String ATT_TYPE = "type";
    static final String ENCODING = "UTF-8";
    static final String VERSION = "1.0";
    private static final String TAG_PARAMETER = "parameter";
    private static final String ATT_NAME = "name";
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");
    private static final Charset CHARSET = Charset.forName(ENCODING);
    private static final ThreadLocal<StreamedPMapParser> _threadLocalData = ThreadLocal.withInitial(StreamedPMapParser::new);
    private static final int MAX_INDENT_LEVEL_CACHE = 256;
    @SuppressWarnings("unchecked")
    private static final WeakReference<String>[] _indentCache = new WeakReference[MAX_INDENT_LEVEL_CACHE];
    private static final IntFunction<String> INDENT_STRING_GENERATOR = i -> "\n"
            + StringUtils.repeat('\t', i);
    private final XMLInputFactory _xmlInputFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory _xmlOutputFactory = XMLOutputFactory.newInstance();
    private final DocumentBuilderFactory _docBuilderFactory = DocumentBuilderFactory.newInstance();
    private final SimpleDateFormat _dateFormatter;
    private DocumentBuilder _documentBuilder;
    private Map<String, PMapReadPlugin> _plugins;

    private StreamedPMapParser() {

        _dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        _dateFormatter.setTimeZone(TIMEZONE_UTC);
        _dateFormatter.setLenient(false);
        _plugins = Collections.emptyMap();
    }

    public StreamedPMapParser(PMapReadPlugin[] plugins) {

        this();
        _plugins = Maps.newHashMapWithExpectedSize(plugins.length);
        for (PMapReadPlugin plugin : plugins) {
            for (String tagName : plugin.getSupportedTags()) {
                _plugins.put(tagName, plugin);
            }
        }
    }

    public static StreamedPMapParser getInstance() {

        return _threadLocalData.get();
    }

    public static void clearCachedInstance() {

        _threadLocalData.remove();
    }

    private static void nextStartElement(XMLStreamReader reader) throws XMLStreamException {

        // Bypass initial elements till we get to start element
        do {
            reader.next();
        } while (reader.getEventType() != XMLStreamReader.START_ELEMENT);
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

        WeakReference<String> container = _indentCache[level];
        String value = container == null ? null : container.get();
        if (value == null) {
            value = INDENT_STRING_GENERATOR.apply(level);
            _indentCache[level] = new WeakReference<>(value);
        }

        return value;
    }

    private static void closeQuietly(XMLStreamReader value) {

        try {
            value.close();
        } catch (XMLStreamException e) {
        }
    }

    private static void closeQuietly(XMLStreamWriter value) {

        try {
            value.close();
        } catch (XMLStreamException e) {
        }
    }

    public static boolean isLeaf(XMLStreamReader xmlStreamReader, PMapType pMapType) {

        return pMapType != null && xmlStreamReader.isStartElement() && !pMapType.equals(PMapType.ARRAY)
                && !pMapType.equals(PMapType.MAP);
    }

    /**
     * return a subTree inside array(XMLStreamReader) representation that match with tagType and keyValues
     *
     * @param xmlStreamReader
     * @param tagType         (XML tag name)
     * @param keyValues       (relation : XML attributes and values)
     * @return
     * @throws XMLStreamException
     */
    public static XMLStreamReader getXMLTreeByArray(XMLStreamReader xmlStreamReader, PMapType tagType,
                                                    ParameterMap keyValues)
            throws XMLStreamException {

        XMLStreamReader result = null;

        do {
            xmlStreamReader.next();
            if (xmlStreamReader.isStartElement()) {
                result = getXMLTreeByTree(xmlStreamReader, tagType, keyValues);
            }
        } while (xmlStreamReader.hasNext() && !xmlStreamReader.isEndElement()
                && xmlStreamReader.getEventType() != XMLStreamConstants.END_DOCUMENT && result == null);

        return result;
    }

    /**
     * return a subTree inside Map(XMLStreamReader) representation that match with tagType and keyValues
     *
     * @param xmlStreamReader
     * @param tagType         (XML tag name)
     * @param keyValues       (relation : XML attributes and values)
     * @return
     * @throws XMLStreamException
     */
    public static XMLStreamReader getXMLTreeByMap(XMLStreamReader xmlStreamReader, PMapType tagType,
                                                  ParameterMap keyValues)
            throws XMLStreamException {

        XMLStreamReader result = null;
        do {
            xmlStreamReader.next();
            if (xmlStreamReader.isStartElement()) {
                result = getXMLTreeByTree(xmlStreamReader, tagType, keyValues);
            }
        } while (xmlStreamReader.hasNext() && !xmlStreamReader.isEndElement()
                && xmlStreamReader.getEventType() != XMLStreamConstants.END_DOCUMENT && result == null);

        return result;
    }

    /**
     * predicate to check if a XML Tree match with given tagType and attribute values (keyValues)
     *
     * @param xmlStreamReader
     * @param tagType
     * @param keyValues
     * @return
     */
    public static boolean matchXMLTree(XMLStreamReader xmlStreamReader, PMapType tagType, ParameterMap keyValues) {

        boolean result = false;
        if (xmlStreamReader.isStartElement() && PMapType.lookup(xmlStreamReader.getLocalName()).equals(tagType)
                && !keyValues.isEmpty()) {
            Iterator<String> keySetI = keyValues.keySet().iterator();
            do {
                String key = keySetI.next();
                String value = ParameterMapUtils.getString(key, keyValues);
                String xmlValue = xmlStreamReader.getAttributeValue(null, key);
                result = xmlValue != null && xmlValue.equals(value);
            } while (keySetI.hasNext() && result);

        }

        return result;
    }

    public static XMLStreamReader nextBrother(XMLStreamReader xmlStreamReader) throws XMLStreamException {

        if (xmlStreamReader.isStartElement()) {

            int control = 1;

            while (!(control == 0 && xmlStreamReader.isEndElement())) {
                xmlStreamReader.next();
                if (xmlStreamReader.isStartElement()) {
                    control++;
                } else if (xmlStreamReader.isEndElement()) {
                    control--;
                }
            }
        }

        int eventType = xmlStreamReader.getEventType();
        if (eventType != XMLStreamConstants.END_DOCUMENT) {
            do {
                eventType = xmlStreamReader.next();
            } while (!xmlStreamReader.isStartElement() && eventType != XMLStreamConstants.END_DOCUMENT);
        }

        return xmlStreamReader;
    }

    public static XMLStreamReader getXMLTreeByTree(XMLStreamReader xmlStreamReader, PMapType tagType,
                                                   ParameterMap keyValues)
            throws XMLStreamException {

        XMLStreamReader result = null;

        if (xmlStreamReader.isStartElement()) {

            if (matchXMLTree(xmlStreamReader, tagType, keyValues)) {
                result = xmlStreamReader;
            } else {
                String name = xmlStreamReader.getName().toString();
                PMapType pMapType = PMapType.lookup(name);

                if (pMapType.equals(PMapType.ARRAY)) {
                    result = getXMLTreeByArray(xmlStreamReader, tagType, keyValues);
                } else if (pMapType.equals(PMapType.MAP)) {
                    result = getXMLTreeByMap(xmlStreamReader, tagType, keyValues);
                }
            }
        }

        return result;
    }

    public static XMLStreamReader getXMLTreeByDocument(XMLStreamReader xmlStreamReaderDoc, PMapType tagType,
                                                       ParameterMap keyValues)
            throws XMLStreamException {

        XMLStreamReader result = null;

        if (xmlStreamReaderDoc.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            xmlStreamReaderDoc.next();

            result = getXMLTreeByTree(xmlStreamReaderDoc, tagType, keyValues);
        }

        return result;
    }

    public static XMLStreamReader getFirstElementByArray(XMLStreamReader xmlStreamReaderArray)
            throws XMLStreamException {

        PMapType pMapType = PMapType.lookup(xmlStreamReaderArray.getLocalName());

        if (xmlStreamReaderArray.isStartElement() && pMapType.equals(PMapType.ARRAY)) {
            int eventType = xmlStreamReaderArray.getEventType();
            do {
                xmlStreamReaderArray.next();
                eventType = xmlStreamReaderArray.getEventType();
            } while (!xmlStreamReaderArray.isStartElement() && eventType != XMLStreamConstants.END_DOCUMENT
                    && !xmlStreamReaderArray.isEndElement());
        }
        return xmlStreamReaderArray;
    }

    public static XMLStreamReader getElementByArray(XMLStreamReader xmlStreamReaderArray, int n)
            throws XMLStreamException {

        PMapType pMapType = PMapType.lookup(xmlStreamReaderArray.getLocalName());

        xmlStreamReaderArray = getFirstElementByArray(xmlStreamReaderArray);

        if (xmlStreamReaderArray != null && xmlStreamReaderArray.isStartElement() && pMapType.equals(PMapType.ARRAY)
                && n > 0) {
            int i = 0;
            while (i < n && !xmlStreamReaderArray.isEndElement()) {
                xmlStreamReaderArray = nextBrother(xmlStreamReaderArray);
                i++;
            }
        }

        return xmlStreamReaderArray;
    }

    public static XMLStreamReader getFirstElementByMap(XMLStreamReader xmlStreamReaderArray) throws XMLStreamException {

        PMapType pMapType = PMapType.lookup(xmlStreamReaderArray.getLocalName());

        if (xmlStreamReaderArray.isStartElement() && pMapType.equals(PMapType.MAP)) {
            int eventType;
            do {
                xmlStreamReaderArray.next();
                eventType = xmlStreamReaderArray.getEventType();
            } while (!xmlStreamReaderArray.isStartElement() && eventType != XMLStreamConstants.END_DOCUMENT
                    && !xmlStreamReaderArray.isEndElement());
        }
        return xmlStreamReaderArray;
    }

    public ParameterMap getMapFromFile(String fileName) throws XMLStreamException, IOException {

        return getMap(new File(fileName));
    }

    public ParameterMap getMap(File file) throws XMLStreamException, IOException {

        try (Reader reader = new BufferedReader(new FileReader(file), 8192)) {
            return getMap(reader);
        }
    }

    public ParameterMap getMap(Path path) throws XMLStreamException, IOException {

        try (InputStream is = path.toUri().toURL().openStream()) {
            return getMap(is);
        }
    }

    public ParameterMap getMap(Reader reader) throws XMLStreamException {

        XMLStreamReader r = _xmlInputFactory.createXMLStreamReader(reader);
        try {
            // Bypass initial elements till we get to start element
            nextStartElement(r);
            return getMap(r);
        } finally {
            closeQuietly(r);
        }
    }

    public ParameterMap getMap(InputStream is) throws XMLStreamException {

        if (is == null) {
            return null;
        }

        XMLStreamReader r = _xmlInputFactory.createXMLStreamReader(is);
        try {
            // Bypass initial elements till we get to start element
            nextStartElement(r);

            return getMap(r);
        } finally {
            closeQuietly(r);
        }
    }

    public ParameterMap getMap(XMLStreamReader reader) throws XMLStreamException {

        reader.next();

        Map<String, Object> omap = new HashMap<>();
        readMap(reader, omap);

        return new ParameterMap(omap);
    }

    public void readMap(XMLStreamReader reader, Map<String, Object> map) throws XMLStreamException {

        while (reader.hasNext()) {

            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                readParam(reader, map);
            } else if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
                break;
            } else {
                reader.nextTag();
            }
        }
    }

    private void readParam(XMLStreamReader reader, Map<String, Object> map) throws XMLStreamException {

        String name = StaxUtils.ATT(reader, ATT_NAME_SHORT);
        if (name == null) {
            name = StaxUtils.ATT(reader, ATT_NAME);
        }
        map.put(name, parseValue(reader));
    }

    private Object parseValue(XMLStreamReader reader) throws XMLStreamException {

        // Validate parameter tag
        String type = StaxUtils.ATT(reader, ATT_TYPE);
        if (type == null) {
            type = reader.getName().getLocalPart(); // Use the tag name if
            // attribute is not
            // available
        }

        PMapType ptype = PMapType.lookup(type);

        if (ptype != null) {

            // Positioning on child element TEXT or <parameter
            String text = StaxUtils.TAG_TEXT(reader);

            try {
                switch (ptype) {
                    case STRING:
                        return text;
                    case INT:
                        return Integer.valueOf(text);
                    case LONG:
                        return Long.valueOf(text);
                    case FLOAT:
                        return Float.valueOf(text);
                    case DOUBLE:
                        return Double.valueOf(text);
                    case BOOLEAN:
                        return Boolean.valueOf(text);
                    case DATE:
                        return parseDate(text);
                    case NULL:
                        return null;
                    case MAP:
                        Map<String, Object> innerMap = new HashMap<>();
                        readMap(reader, innerMap);
                        return new ParameterMap(innerMap);
                    case ARRAY:
                        return parseList(reader);
                    case DECIMAL:
                        return new BigDecimal(text);
                }
            } catch (Exception exc) {
                throw new XMLStreamException("Invalid data -> " + reader.getEventType() + "-" + exc, exc);
            } finally {
                reader.next();
            }
        } else {
            if (!_plugins.isEmpty()) {
                PMapReadPlugin readPlugin = _plugins.get(type);
                if (readPlugin != null) {
                    try {
                        return readPlugin.readObject(type, reader);
                    } catch (IOException e) {
                        throw new XMLStreamException("Invalid data -> " + reader.getEventType() + "-" + e);
                    } finally {
                        reader.next();
                    }
                }
            }
        }

        throw new XMLStreamException("Invalid type - " + type);
    }

    private List<Object> parseList(XMLStreamReader reader) throws XMLStreamException, SatelliteException {

        List<Object> innerList = new ArrayList<>();

        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                innerList.add(parseValue(reader));
            } else if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
                break;
            } else {
                reader.nextTag();
            }

        }

        return innerList;
    }

    private Date parseDate(String text) throws ParseException {

        try {
            return _dateFormatter.parse(text);
        } catch (ParseException e) {
            //Previous snapshot expireDate were created in the future, with an unparseable date
            if ("2922789940817071255".equals(text)) {
                return new Date(Long.MAX_VALUE);
            }
            throw e;
        }

    }

    private DocumentFragment parseXML(XMLStreamReader reader) throws XMLStreamException, ParserConfigurationException {

        if (_documentBuilder == null) {
            _documentBuilder = _docBuilderFactory.newDocumentBuilder();
        }
        Document doc = _documentBuilder.newDocument();

        DocumentFragment df = doc.createDocumentFragment();
        Node node = doc.createElement("dummy");

        StaxUtils.XMLReader2Document(df.getOwnerDocument(), node, reader);

        // Import all children nodes and their structure
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node nodec = doc.importNode(children.item(i), true);
            df.appendChild(nodec);
        }

        return df;
    }

    public byte[] PMAPtoByteArray(Map<String, Object> map, SerializationType type)
            throws XMLStreamException, IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        PMAPtoOutputStream(map, type, bos);
        bos.flush();

        return bos.toByteArray();
    }

    public void PMAPtoOutputStream(Map<String, Object> map, SerializationType type, OutputStream os)
            throws XMLStreamException, IOException {

        try (OutputStreamWriter writer = new OutputStreamWriter(os, CHARSET)) {
            PMAPtoWriter(map, type, writer);
        }
    }

    public void PMAPtoWriter(Map<String, Object> map, SerializationType type, Writer w)
            throws XMLStreamException, IOException {

        final XMLStreamWriter writer = _xmlOutputFactory.createXMLStreamWriter(w);
        try {
            writer.writeStartDocument(ENCODING, VERSION);
            writer.writeCharacters("\n");
            if (type.getVersion() == 1) {
                writer.writeStartElement(PMapType.MAP.getOldPMapName());
            } else {
                writer.writeStartElement(PMapType.MAP.getShortName());
            }
            XMLWriterToMapWithoutRoot(type, writer, map, 0);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } finally {
            closeQuietly(writer);
        }
    }

    public String toXMLString(Map<String, Object> map, SerializationType type) throws XMLStreamException {

        if (map == null) {
            return null;
        }

        try (StringWriter sw = new StringWriter()) {
            PMAPtoWriter(map, type, sw);
            sw.flush();
            return sw.toString();
        } catch (IOException ioe) {
            throw new CoreError(ioe.toString(), ioe);
        }
    }

    public ParameterMap ByteArrayToPMAP(SerializationType serType, byte[] content)
            throws XMLStreamException, IOException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(content)) {
            return InputStreamToPMAP(serType, bis);
        }
    }

    public ParameterMap InputStreamToPMAP(SerializationType serType, InputStream is)
            throws XMLStreamException, IOException {

        XMLStreamReader reader = _xmlInputFactory.createXMLStreamReader(new InputStreamReader(is, CHARSET));
        try {
            final String pname = serType.getVersion() == 1 ? PMapType.MAP.getOldPMapName()
                    : PMapType.MAP.getShortName();

            reader.nextTag();
            String rootTag = reader.getLocalName();

            if ((rootTag != null && rootTag.equals(pname))
                    || (serType.getVersion() == 1 && pname != null
                    && pname.equals(reader.getAttributeValue(null, "type")))) {
                return getMap(reader);
            }
        } finally {
            closeQuietly(reader);
        }

        throw new XMLStreamException("unknown pmap format");
    }

    public void XMLWriterToMapWithoutRoot(SerializationType serType, XMLStreamWriter writer, Map<String, Object> map)
            throws XMLStreamException {

        XMLWriterToMapWithoutRoot(serType, writer, map, 0);
    }

    public void XMLWriterToMapWithoutRoot(SerializationType serType, XMLStreamWriter writer, Map<String, ?> map,
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
            XMLWriterToValue(serType, writer, key, map.get(key), levelBelow);
        }

        indentLevel(serType, writer, level);
    }

    private void XMLWriterToList(SerializationType serType, XMLStreamWriter writer, Collection<?> list, int level)
            throws XMLStreamException {

        int levelBelow = level + 1;

        for (Object value : list) {
            XMLWriterToValue(serType, writer, null, value, levelBelow);
        }
        indentLevel(serType, writer, level);
    }

    @SuppressWarnings("unchecked")
    private void XMLWriterToValue(SerializationType serType, XMLStreamWriter writer, String key, Object value,
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
            writer.writeAttribute(ATT_TYPE, type.getOldPMapName());
            if (key != null) {
                writer.writeAttribute(ATT_NAME, key);
            }
        } else {
            indentLevel(serType, writer, level);
            writer.writeStartElement(type.getShortName());
            if (key != null) {
                writer.writeAttribute(ATT_NAME_SHORT, key);
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
                writeSimpleValue(writer, type, value);
                break;
            case MAP:
                XMLWriterToMapWithoutRoot(serType, writer, (Map<String, Object>) value, level);
                break;
            case ARRAY:
                XMLWriterToList(serType, writer, (Collection<?>) value, level);
                break;
            case DATE:
                writeSimpleValue(writer, type, _dateFormatter.format(value));
                break;
            case NULL:
                break;
        }
        writer.writeEndElement();

    }

    private void writeSimpleValue(XMLStreamWriter writer, PMapType type, Object value) throws XMLStreamException {

        writer.writeCharacters(String.valueOf(value));
    }

    public Object parseValueLeaf(String value, PMapType ptype) throws ParseException {

        Object object = null;

        switch (ptype) {
            case STRING:
                object = value;
                break;
            case INT:
                object = Integer.parseInt(value);
                break;
            case LONG:
                object = Long.parseLong(value);
                break;
            case FLOAT:
                object = Float.parseFloat(value);
                break;
            case DOUBLE:
                object = Double.parseDouble(value);
                break;
            case BOOLEAN:
                object = Boolean.parseBoolean(value);
                break;
            case DATE:
                object = _dateFormatter.parse(value);
                break;
            case DECIMAL:
                object = new BigDecimal(value);
                break;
            default:
                break;
        }

        return object;
    }

    public Entry<String, Object> parseLeaf(XMLStreamReader xmlStreamReader, PMapType ptype)
            throws XMLStreamException, ParseException {

        String key = xmlStreamReader.getAttributeValue(0);

        Map.Entry<String, Object> result = new AbstractMap.SimpleEntry<>(key, null);

        xmlStreamReader.next();

        if (!ptype.equals(PMapType.NULL)) {

            String value = xmlStreamReader.getText();

            while (xmlStreamReader.hasNext() && xmlStreamReader.next() != XMLStreamConstants.END_ELEMENT) {
                value = value.concat(xmlStreamReader.getText());
            }

            if (value != null && !value.isEmpty() && ptype != null && !ptype.equals(PMapType.NULL)) {
                result.setValue(parseValueLeaf(value, ptype));
            }
        }

        xmlStreamReader.next();

        return result;
    }

    public ParameterMap parseArray(XMLStreamReader xmlStreamReader) throws XMLStreamException, ParseException {

        String keyA = "";
        if (xmlStreamReader.getAttributeCount() > 0) {
            keyA = xmlStreamReader.getAttributeValue(0);
        }

        List<ParameterMap> pMapList = new ArrayList<>();
        do {
            xmlStreamReader.next();
            if (xmlStreamReader.isStartElement()) {
                ParameterMap map = parseTree(xmlStreamReader);
                pMapList.add(map);
            }
        } while (xmlStreamReader.hasNext() && !xmlStreamReader.isEndElement()
                && xmlStreamReader.getEventType() != XMLStreamConstants.END_DOCUMENT);

        ParameterMap result = new ParameterMap();
        result.put(keyA, pMapList);
        xmlStreamReader.next();

        return result;
    }

    public ParameterMap parseMap(XMLStreamReader xmlStreamReader) throws XMLStreamException, ParseException {


        String keyM = "";
        if (xmlStreamReader.getAttributeCount() > 0) {
            keyM = xmlStreamReader.getAttributeValue(0);
        }
        ParameterMap valueM = new ParameterMap();

        xmlStreamReader = getFirstElementByMap(xmlStreamReader);

        while (xmlStreamReader.hasNext() && !xmlStreamReader.isEndElement()
                && xmlStreamReader.getEventType() != XMLStreamConstants.END_DOCUMENT) {
            if (xmlStreamReader.isStartElement()) {
                valueM.putAll(parseTree(xmlStreamReader));
            } else {
                xmlStreamReader.next();
            }
        }

        final ParameterMap result;
        if (keyM == null || keyM.isEmpty()) {
            result = valueM;
        } else {
            result = new ParameterMap();
            result.put(keyM, valueM);
        }

        if (xmlStreamReader.hasNext()) {
            xmlStreamReader.next();
        }

        return result;
    }

    public ParameterMap parseTree(XMLStreamReader xmlStreamReader) throws XMLStreamException, ParseException {

        ParameterMap result = new ParameterMap();

        if (xmlStreamReader.isStartElement()) {

            PMapType pMapType = PMapType.lookup(xmlStreamReader.getName().toString());

            if (isLeaf(xmlStreamReader, pMapType)) {
                Entry<String, Object> entry = parseLeaf(xmlStreamReader, pMapType);
                result.put(entry.getKey(), entry.getValue());
            } else if (pMapType.equals(PMapType.ARRAY)) {
                result = parseArray(xmlStreamReader);
            } else if (pMapType.equals(PMapType.MAP)) {
                result = parseMap(xmlStreamReader);
            }
        }

        return result;

    }

    public ParameterMap parseDocument(XMLStreamReader xmlStreamReader) throws XMLStreamException, ParseException {

        ParameterMap result = new ParameterMap();

        if (xmlStreamReader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            xmlStreamReader.next();

            result = parseTree(xmlStreamReader);
        }

        return result;

    }

    public List<ParameterMap> getRangeMapListByArray(XMLStreamReader xmlStreamReaderArray, int indexStart, int indexEnd)
            throws XMLStreamException, CoreException, ParseException {

        List<ParameterMap> result = new ArrayList<>();

        if (xmlStreamReaderArray.isStartElement()) {
            PMapType pMapType = PMapType.lookup(xmlStreamReaderArray.getLocalName());
            if (pMapType.equals(PMapType.ARRAY) && indexStart > -1 && indexEnd >= 0 && indexStart <= indexEnd) {

                xmlStreamReaderArray = getElementByArray(xmlStreamReaderArray, indexStart);
                int i = indexStart;
                while (i <= indexEnd && xmlStreamReaderArray.hasNext() && !xmlStreamReaderArray.isEndElement()) {

                    if (xmlStreamReaderArray.isStartElement()) {
                        result.add(parseTree(xmlStreamReaderArray));
                        i++;
                    } else {
                        xmlStreamReaderArray.next();
                    }
                }
            }
        }

        return result;
    }

    public List<ParameterMap> getRangeListByArray(XMLStreamReader initialPosition, int numberOfRecords)
            throws SatelliteException, XMLStreamException, ParseException {

        List<ParameterMap> result = new ArrayList<>();

        if (initialPosition.isStartElement()) {

            int i = 0;
            while (i < numberOfRecords && initialPosition.hasNext() && !initialPosition.isEndElement()
                    && initialPosition.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                if (initialPosition.isStartElement()) {
                    ParameterMap map = parseTree(initialPosition);
                    result.add(map);
                    i++;
                } else {
                    initialPosition.next();
                }
            }

        }

        return result;
    }


    public enum SerializationType {
        PMAP1(true, false, 1),
        PMAP2(false, false, 2),
        PMAP2_WITH_FORMATTING(false, true, 2),
        PMAP2_NO_UNKNOWN(true, false, 2),
        PMAP1_NO_UNKWNOWN(true, false, 1);

        private final boolean _ignoreUnknown;
        private final boolean _indent;
        private final int _version;


        SerializationType(boolean ignoreUnknownValue, boolean indent, int version) {

            _ignoreUnknown = ignoreUnknownValue;
            _indent = indent;
            _version = version;
        }


        public int getVersion() {

            return _version;
        }


        public boolean ignoreUnknownTypes() {

            return _ignoreUnknown;
        }

        public boolean ident() {

            return _indent;
        }
    }


}
