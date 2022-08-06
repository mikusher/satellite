package com.mikusher.utils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

public interface PMapReadPlugin {

    String[] getSupportedTags();

    Object readObject(String type, XMLStreamReader reader) throws XMLStreamException, IOException;
}