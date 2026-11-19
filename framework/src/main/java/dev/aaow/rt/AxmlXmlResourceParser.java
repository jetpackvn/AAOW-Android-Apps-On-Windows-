package dev.aaow.rt;

import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.ResourceTable;
import dev.aaow.apk.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt XmlResourceParser & XmlPullParser dựa trên cây XmlElement của AAOW.
 * Hỗ trợ cả XML nhị phân (AXML từ APK) lẫn XML thuần văn bản qua Xml.newPullParser().
 */
public final class AxmlXmlResourceParser implements XmlResourceParser {
    private record Event(int type, int depth, XmlElement element, String text) {
    }

    private final ResourceTable resources;
    private final List<Event> events = new ArrayList<>();
    private XmlElement rootElement;
    private int cursor = 0;
    private String inputEncoding = "UTF-8";
    private boolean processNamespaces = true;

    public AxmlXmlResourceParser(XmlElement root, ResourceTable resources) {
        this.resources = resources != null ? resources : ResourceTable.empty();
        this.rootElement = root;
        buildEvents(root);
    }

    public XmlElement getRootElement() {
        return rootElement;
    }

    public XmlElement getCurrentXmlElement() {
        return currentElement();
    }

    public AxmlXmlResourceParser() {
        this.resources = ResourceTable.empty();
        this.events.add(new Event(START_DOCUMENT, 0, null, null));
        this.events.add(new Event(END_DOCUMENT, 0, null, null));
    }

    private void buildEvents(XmlElement root) {
        events.clear();
        cursor = 0;
        events.add(new Event(START_DOCUMENT, 0, null, null));
        if (root != null) {
            appendElement(root, 1);
        }
        events.add(new Event(END_DOCUMENT, 0, null, null));
    }

    private void appendElement(XmlElement el, int depth) {
        events.add(new Event(START_TAG, depth, el, null));
        if (el.text != null && !el.text.isEmpty()) {
            events.add(new Event(TEXT, depth, el, el.text));
        }
        for (XmlElement child : el.children) {
            appendElement(child, depth + 1);
        }
        events.add(new Event(END_TAG, depth, el, null));
    }

    private Event current() {
        if (cursor < 0) return events.get(0);
        if (cursor >= events.size()) return events.get(events.size() - 1);
        return events.get(cursor);
    }

    private XmlElement currentElement() {
        return current().element();
    }

    private AttributeSet.XmlAttributeSet currentAttrSet() {
        XmlElement el = currentElement();
        return el != null ? new AttributeSet.XmlAttributeSet(el, resources) : null;
    }

    // --------------------------------------------------------- XmlPullParser

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        if (FEATURE_PROCESS_NAMESPACES.equals(name)) {
            this.processNamespaces = state;
        }
    }

    @Override
    public boolean getFeature(String name) {
        if (FEATURE_PROCESS_NAMESPACES.equals(name)) {
            return processNamespaces;
        }
        return false;
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        if (in == null) throw new XmlPullParserException("Reader is null");
        parsePlainXml(new InputSource(in));
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
        if (inputStream == null) throw new XmlPullParserException("InputStream is null");
        this.inputEncoding = inputEncoding != null ? inputEncoding : "UTF-8";
        InputSource src = new InputSource(inputStream);
        if (inputEncoding != null) src.setEncoding(inputEncoding);
        parsePlainXml(src);
    }

    private void parsePlainXml(InputSource source) throws XmlPullParserException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            Document doc = dbf.newDocumentBuilder().parse(source);
            XmlElement root = convertDom(doc.getDocumentElement());
            buildEvents(root);
        } catch (Exception e) {
            throw new XmlPullParserException("Failed to parse XML input", this, e);
        }
    }

    private static XmlElement convertDom(Element dom) {
        if (dom == null) return null;
        String ns = dom.getNamespaceURI();
        String name = dom.getLocalName() != null ? dom.getLocalName() : dom.getTagName();
        XmlElement el = new XmlElement(ns, name, 1);
        NamedNodeMap attrs = dom.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            String ans = a.getNamespaceURI();
            if ("http://www.w3.org/2000/xmlns/".equals(ans)) continue;
            String aname = a.getLocalName() != null ? a.getLocalName() : a.getNodeName();
            el.attributes.add(new XmlElement.Attribute(
                    ans, aname, 0, new ResValue(ResValue.TYPE_STRING, 0, a.getNodeValue())));
        }
        StringBuilder textBuf = new StringBuilder();
        NodeList children = dom.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element childEl) {
                el.children.add(convertDom(childEl));
            } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                String txt = n.getNodeValue();
                if (txt != null && !txt.isBlank()) {
                    textBuf.append(txt);
                }
            }
        }
        if (textBuf.length() > 0) {
            el.text = textBuf.toString();
        }
        return el;
    }

    @Override
    public String getInputEncoding() {
        return inputEncoding;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText) {
    }

    @Override
    public int getNamespaceCount(int depth) {
        return 0;
    }

    @Override
    public String getNamespacePrefix(int pos) {
        return null;
    }

    @Override
    public String getNamespaceUri(int pos) {
        return null;
    }

    @Override
    public String getNamespace(String prefix) {
        if ("android".equals(prefix)) return XmlElement.ANDROID_NS;
        if ("app".equals(prefix)) return XmlElement.AUTO_NS;
        return null;
    }

    @Override
    public int getDepth() {
        return current().depth();
    }

    @Override
    public String getPositionDescription() {
        XmlElement el = currentElement();
        return el != null ? "<" + el.name + " line=" + el.line + ">" : TYPES[getEventType()];
    }

    @Override
    public int getLineNumber() {
        XmlElement el = currentElement();
        return el != null ? el.line : -1;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    @Override
    public boolean isWhitespace() {
        if (current().type() != TEXT) return false;
        String t = current().text();
        return t == null || t.isBlank();
    }

    @Override
    public String getText() {
        return current().type() == TEXT ? current().text() : null;
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        String t = getText();
        if (t == null) {
            if (holderForStartAndLength != null && holderForStartAndLength.length >= 2) {
                holderForStartAndLength[0] = -1;
                holderForStartAndLength[1] = 0;
            }
            return null;
        }
        if (holderForStartAndLength != null && holderForStartAndLength.length >= 2) {
            holderForStartAndLength[0] = 0;
            holderForStartAndLength[1] = t.length();
        }
        return t.toCharArray();
    }

    @Override
    public String getNamespace() {
        XmlElement el = currentElement();
        if (el == null) return "";
        return el.namespace != null ? el.namespace : "";
    }

    @Override
    public String getName() {
        XmlElement el = currentElement();
        return el != null ? el.name : null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public boolean isEmptyElementTag() {
        XmlElement el = currentElement();
        return el != null && el.children.isEmpty() && (el.text == null || el.text.isEmpty());
    }

    @Override
    public int getAttributeCount() {
        if (current().type() != START_TAG) return -1;
        XmlElement el = currentElement();
        return el != null ? el.attributes.size() : 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        XmlElement el = currentElement();
        if (el == null || index < 0 || index >= el.attributes.size()) return "";
        String ns = el.attributes.get(index).namespace;
        return ns != null ? ns : "";
    }

    @Override
    public String getAttributeName(int index) {
        XmlElement el = currentElement();
        if (el == null || index < 0 || index >= el.attributes.size()) return null;
        return el.attributes.get(index).name;
    }

    @Override
    public String getAttributePrefix(int index) {
        String ns = getAttributeNamespace(index);
        if (XmlElement.ANDROID_NS.equals(ns)) return "android";
        if (XmlElement.AUTO_NS.equals(ns)) return "app";
        return null;
    }

    @Override
    public String getAttributeType(int index) {
        return "CDATA";
    }

    @Override
    public boolean isAttributeDefault(int index) {
        return false;
    }

    @Override
    public String getAttributeValue(int index) {
        XmlElement el = currentElement();
        if (el == null || index < 0 || index >= el.attributes.size()) return null;
        ResValue v = resources.resolve(el.attributes.get(index).value);
        return v != null ? v.coerceToString() : null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        XmlElement el = currentElement();
        if (el == null || name == null) return null;
        String cleanNs = (namespace != null && namespace.isEmpty()) ? null : namespace;
        XmlElement.Attribute a = el.attr(cleanNs, name);
        if (a == null && cleanNs == null) {
            a = el.attr(XmlElement.ANDROID_NS, name);
        }
        if (a == null) return null;
        ResValue v = resources.resolve(a.value);
        return v != null ? v.coerceToString() : null;
    }

    @Override
    public int getEventType() {
        return current().type();
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        if (cursor + 1 < events.size()) {
            cursor++;
        }
        return current().type();
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return next();
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        if (type != getEventType()
                || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException("Expected " + TYPES[type] + " " + name, this, null);
        }
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        if (getEventType() != START_TAG) {
            throw new XmlPullParserException("Precondition for nextText(): START_TAG", this, null);
        }
        int eventType = next();
        if (eventType == TEXT) {
            String result = getText();
            eventType = next();
            if (eventType != END_TAG) {
                throw new XmlPullParserException("Expected END_TAG after text", this, null);
            }
            return result != null ? result : "";
        } else if (eventType == END_TAG) {
            return "";
        }
        throw new XmlPullParserException("Unexpected event after START_TAG: " + eventType, this, null);
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        int eventType = next();
        if (eventType == TEXT && isWhitespace()) {
            eventType = next();
        }
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("Expected START_TAG or END_TAG, got " + eventType, this, null);
        }
        return eventType;
    }

    // --------------------------------------------------------- AttributeSet

    @Override
    public int getAttributeNameResource(int index) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeNameResource(index) : 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeListValue(namespace, attribute, options, defaultValue) : defaultValue;
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeBooleanValue(namespace, attribute, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeResourceValue(namespace, attribute, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeIntValue(namespace, attribute, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeUnsignedIntValue(namespace, attribute, defaultValue) : defaultValue;
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeFloatValue(namespace, attribute, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeListValue(index, options, defaultValue) : defaultValue;
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeBooleanValue(index, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeResourceValue(index, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeIntValue(index, defaultValue) : defaultValue;
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeUnsignedIntValue(index, defaultValue) : defaultValue;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getAttributeFloatValue(index, defaultValue) : defaultValue;
    }

    @Override
    public String getIdAttribute() {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getIdAttribute() : null;
    }

    @Override
    public String getClassAttribute() {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getClassAttribute() : null;
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getIdAttributeResourceValue(defaultValue) : defaultValue;
    }

    @Override
    public int getStyleAttribute() {
        AttributeSet.XmlAttributeSet as = currentAttrSet();
        return as != null ? as.getStyleAttribute() : 0;
    }

    @Override
    public void close() {
    }
}
