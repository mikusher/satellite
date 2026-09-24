package com.mikusher.utils;

import org.w3c.dom.*;

import javax.xml.stream.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class StaxUtils {

    private static final String XML_NS = "http://www.w3.org/2000/xmlns/";


    private StaxUtils() {

    }


    public static final String ATT(XMLStreamReader reader, String att, String def) {

        String value = reader.getAttributeValue("", att);
        if (value == null) {
            return def;
        }
        return value;
    }


    public static final String ATT(XMLStreamReader reader, String att) {

        return reader.getAttributeValue(null, att);
    }

    private static void declare(Element node, String uri, String prefix) {

        String qualname;
        if (prefix != null && prefix.length() > 0) {
            qualname = "xmlns:" + prefix;
        } else {
            qualname = "xmlns";
        }
        Attr attr = node.getOwnerDocument().createAttributeNS(XML_NS, qualname);
        attr.setValue(uri);
        node.setAttributeNodeNS(attr);
    }

    private static boolean isDeclared(Element e, String namespaceURI, String prefix) {

        while (e != null) {
            Attr att;
            if (prefix != null && prefix.length() > 0) {
                att = e.getAttributeNodeNS(XML_NS, prefix);
            } else {
                att = e.getAttributeNode("xmlns");
            }

            if (att != null && att.getNodeValue().equals(namespaceURI)) {
                return true;
            }

            if (e.getParentNode() instanceof Element) {
                e = (Element) e.getParentNode();
            } else {
                // A document that probably doesn't have any namespace qualifies
                // elements
                return prefix != null && prefix.isEmpty() && namespaceURI != null && namespaceURI.isEmpty();
            }
        }
        return false;
    }

    private static boolean addLocation(Document doc, Node node, XMLStreamReader reader, boolean recordLoc) {

        if (!recordLoc) {
            return recordLoc;
        }

        Location loc = reader.getLocation();
        if (loc != null && (loc.getColumnNumber() != 0 || loc.getLineNumber() != 0)) {
            try {
                node.setUserData("location", new InternalLocation(doc, loc), new UserDataHandler() {

                    @Override
                    public void handle(short operation, String key, Object data, Node src, Node dst) {

                        if (operation == NODE_CLONED) {
                            dst.setUserData(key, data, this);
                        }
                    }
                });
            } catch (Exception ex) {
                // possibly not DOM level 3, won't be able to record this
                // then
                return false;
            }
        }

        return recordLoc;
    }

    private static boolean isTextNode(XMLStreamReader reader) {

        final int eventType = reader.getEventType();
        return eventType == XMLStreamReader.CHARACTERS || eventType == XMLStreamReader.ENTITY_REFERENCE || eventType == XMLStreamReader.CDATA || eventType == XMLStreamReader.COMMENT;
    }

    private static String getValidText(XMLStreamReader reader) {

        final int eventType = reader.getEventType();
        if (eventType == XMLStreamReader.CHARACTERS || eventType == XMLStreamReader.CDATA) {
            return reader.getText();
        }

        return null;
    }

    public static String TAG_TEXT(XMLStreamReader reader) throws XMLStreamException {

        reader.next();
        if (reader.isEndElement()) {
            return "";
        }

        return TEXT(reader);
    }

    public static String TEXT(XMLStreamReader reader) throws XMLStreamException {

        StringBuilder text = new StringBuilder(128);
        for (; isTextNode(reader); reader.next()) {
            final String content = getValidText(reader);
            if (content != null) {
                text.append(content);
            }
        }

        return text.toString();
    }

    public static void DOMNode2XMLWriter(Node node, XMLStreamWriter out) throws XMLStreamException {

        DOMNode2XMLWriter("", "", node, out);
    }

    public static void DOMNode2XMLWriter(String ns, String prefix, Node node, XMLStreamWriter out)
            throws XMLStreamException {

        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                visitDocument(ns, prefix, (Document) node, out);
                break;
            case Node.DOCUMENT_FRAGMENT_NODE:
                visitDocumentFragment(ns, prefix, (DocumentFragment) node, out);
                break;
            case Node.ELEMENT_NODE:
                visitElement(ns, prefix, (Element) node, out);
                break;
            case Node.TEXT_NODE:
                visitText((Text) node, out);
                break;
            case Node.CDATA_SECTION_NODE:
                visitCDATASection((CDATASection) node, out);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                visitProcessingInstruction((ProcessingInstruction) node, out);
                break;
            case Node.ENTITY_REFERENCE_NODE:
                visitReference(ns, prefix, (EntityReference) node, out);
                break;
            case Node.COMMENT_NODE:
                visitComment((Comment) node, out);
                break;
            case Node.DOCUMENT_TYPE_NODE:
                break;
            case Node.ATTRIBUTE_NODE:
            case Node.ENTITY_NODE:
            default:
                throw new XMLStreamException("Unexpected DOM Node Type " + node.getNodeType());
        }
    }

    private static void visitChildren(String ns, String prefix, Node node, XMLStreamWriter out)
            throws XMLStreamException {

        final NodeList nodeList = node.getChildNodes();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            DOMNode2XMLWriter(ns, prefix, nodeList.item(i), out);
        }
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitDocument(String ns, String prefix, Document document, XMLStreamWriter out)
            throws XMLStreamException {

        out.writeStartDocument();
        DOMNode2XMLWriter(ns, prefix, document.getDocumentElement(), out);
        out.writeEndDocument();
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitDocumentFragment(String ns, String prefix, DocumentFragment documentFragment,
                                              XMLStreamWriter out) throws XMLStreamException {

        visitChildren(ns, prefix, documentFragment, out);
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitElement(String ns, String prefix, Element node, XMLStreamWriter out)
            throws XMLStreamException {

        out.writeStartElement(prefix, node.getNodeName(), ns);
        NamedNodeMap attrs = node.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            visitAttr(ns, prefix, (Attr) attrs.item(i), out);
        }
        visitChildren(ns, prefix, node, out);
        out.writeEndElement();
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitAttr(String prefix, String ns, Attr node, XMLStreamWriter out) throws XMLStreamException {

        out.writeAttribute(ns, prefix, node.getName(), node.getNodeValue());

    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitComment(Comment comment, XMLStreamWriter out) throws XMLStreamException {

        out.writeComment(comment.getData());
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitText(Text node, XMLStreamWriter out) throws XMLStreamException {

        out.writeCharacters(node.getNodeValue());
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitCDATASection(CDATASection cdata, XMLStreamWriter out) throws XMLStreamException {

        out.writeCData(cdata.getNodeValue());
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitProcessingInstruction(ProcessingInstruction processingInstruction, XMLStreamWriter out)
            throws XMLStreamException {

        out.writeProcessingInstruction(processingInstruction.getNodeName(), processingInstruction.getData());
    }

    /*******************************************************************************
     *
     *
     ******************************************************************************/
    private static void visitReference(String ns, String prefix, EntityReference entityReference, XMLStreamWriter out)
            throws XMLStreamException {

        visitChildren(ns, prefix, entityReference, out);
    }

    public static void XMLReader2Document(Document doc, Node parent, XMLStreamReader reader) throws XMLStreamException {

        XMLReader2Document(doc, parent, reader, false, false, false);
    }

    public static void XMLReader2Document(Document doc, Node parent, XMLStreamReader reader, boolean repairing,
                                          boolean recordLoc, boolean isThreshold) throws XMLStreamException {

        Deque<Node> stack = new ArrayDeque<>();
        int event = reader.getEventType();
        while (reader.hasNext()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    Element e;
                    if (reader.getPrefix() != null && reader.getPrefix().length() > 0) {
                        e = doc.createElementNS(reader.getNamespaceURI(),
                                reader.getPrefix() + ":" + reader.getLocalName());
                    } else {
                        e = doc.createElementNS(reader.getNamespaceURI(), reader.getLocalName());
                    }
                    e = (Element) parent.appendChild(e);
                    recordLoc = addLocation(doc, e, reader, recordLoc);

                    for (int ns = 0; ns < reader.getNamespaceCount(); ns++) {
                        String uri = reader.getNamespaceURI(ns);
                        String prefix = reader.getNamespacePrefix(ns);

                        declare(e, uri, prefix);
                    }

                    for (int att = 0; att < reader.getAttributeCount(); att++) {
                        String name = reader.getAttributeLocalName(att);
                        String prefix = reader.getAttributePrefix(att);
                        if (prefix != null && prefix.length() > 0) {
                            name = prefix + ":" + name;
                        }

                        Attr attr = doc.createAttributeNS(reader.getAttributeNamespace(att), name);
                        attr.setValue(reader.getAttributeValue(att));
                        e.setAttributeNode(attr);
                    }

                    if (repairing && !isDeclared(e, reader.getNamespaceURI(), reader.getPrefix())) {
                        declare(e, reader.getNamespaceURI(), reader.getPrefix());
                    }
                    stack.push(parent);

                    parent = e;
                    break;
                }
                case XMLStreamConstants.END_ELEMENT:
                    if (stack.isEmpty()) {
                        return;
                    }
                    parent = stack.pop();
                    if (parent instanceof Document) {
                        return;
                    }
                    break;
                case XMLStreamConstants.NAMESPACE:
                    break;
                case XMLStreamConstants.ATTRIBUTE:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (parent != null) {
                        recordLoc = addLocation(doc,
                                parent.appendChild(doc.createTextNode(reader.getText())),
                                reader,
                                recordLoc);
                    }
                    break;
                case XMLStreamConstants.COMMENT:
                    if (parent != null) {
                        parent.appendChild(doc.createComment(reader.getText()));
                    }
                    break;
                case XMLStreamConstants.CDATA:
                    recordLoc = addLocation(doc,
                            parent.appendChild(doc.createCDATASection(reader.getText())),
                            reader,
                            recordLoc);
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                default:
                    break;
            }

            if (reader.hasNext()) {
                event = reader.next();
            }
        }
    }

    private static final class InternalLocation implements Location {

        private final int _charOffset;
        private final int _colNum;
        private final int _lineNum;
        private final String _pubId;
        private final String _sysId;


        private InternalLocation(Document doc, Location loc) {

            _charOffset = loc.getCharacterOffset();
            _colNum = loc.getColumnNumber();
            _lineNum = loc.getLineNumber();
            _pubId = loc.getPublicId() == null ? doc.getDocumentURI() : loc.getPublicId();
            _sysId = loc.getSystemId() == null ? doc.getDocumentURI() : loc.getSystemId();
        }


        @Override
        public int getLineNumber() {

            return _lineNum;
        }


        @Override
        public int getColumnNumber() {

            return _colNum;
        }


        @Override
        public int getCharacterOffset() {

            return _charOffset;
        }


        @Override
        public String getPublicId() {

            return _pubId;
        }


        @Override
        public String getSystemId() {

            return _sysId;
        }

    }


}
