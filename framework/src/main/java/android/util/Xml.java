package android.util;

import dev.aaow.rt.AxmlXmlResourceParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

public class Xml {
    public static String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";

    public enum Encoding {
        US_ASCII("US-ASCII"),
        UTF_8("UTF-8"),
        UTF_16("UTF-16"),
        ISO_8859_1("ISO-8859-1");

        final String expatName;

        Encoding(String expatName) {
            this.expatName = expatName;
        }
    }

    public static XmlPullParser newPullParser() {
        return new AxmlXmlResourceParser();
    }

    public static AttributeSet asAttributeSet(XmlPullParser parser) {
        if (parser instanceof AttributeSet as) {
            return as;
        }
        return new PullParserAttributeSet(parser);
    }

    public static Encoding findEncodingByName(String encodingName) throws java.io.UnsupportedEncodingException {
        if (encodingName == null) return Encoding.UTF_8;
        for (Encoding e : Encoding.values()) {
            if (e.expatName.equalsIgnoreCase(encodingName) || e.name().equalsIgnoreCase(encodingName)) {
                return e;
            }
        }
        throw new java.io.UnsupportedEncodingException(encodingName);
    }

    public static void parse(String xml, ContentHandler contentHandler) throws SAXException {
        try {
            parse(new StringReader(xml != null ? xml : ""), contentHandler);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public static void parse(Reader in, ContentHandler contentHandler) throws IOException, SAXException {
        parseSource(new InputSource(in), contentHandler);
    }

    public static void parse(InputStream in, Encoding encoding, ContentHandler contentHandler)
            throws IOException, SAXException {
        InputSource src = new InputSource(in);
        if (encoding != null) src.setEncoding(encoding.expatName);
        parseSource(src, contentHandler);
    }

    private static void parseSource(InputSource src, ContentHandler contentHandler) throws IOException, SAXException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setContentHandler(contentHandler);
            reader.parse(src);
        } catch (SAXException e) {
            throw e;
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    private static final class PullParserAttributeSet implements AttributeSet {
        private final XmlPullParser p;

        PullParserAttributeSet(XmlPullParser parser) {
            this.p = parser;
        }

        @Override
        public int getAttributeCount() {
            return p.getAttributeCount();
        }

        @Override
        public String getAttributeNamespace(int index) {
            return p.getAttributeNamespace(index);
        }

        @Override
        public String getAttributeName(int index) {
            return p.getAttributeName(index);
        }

        @Override
        public String getAttributeValue(int index) {
            return p.getAttributeValue(index);
        }

        @Override
        public String getAttributeValue(String namespace, String name) {
            return p.getAttributeValue(namespace, name);
        }

        @Override
        public String getPositionDescription() {
            return p.getPositionDescription();
        }

        @Override
        public int getAttributeNameResource(int index) {
            return 0;
        }

        @Override
        public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
            String v = getAttributeValue(namespace, attribute);
            return v != null ? Boolean.parseBoolean(v) : defaultValue;
        }

        @Override
        public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
            return defaultValue;
        }

        @Override
        public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
            String v = getAttributeValue(namespace, attribute);
            if (v == null) return defaultValue;
            try {
                return Integer.decode(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
            return getAttributeIntValue(namespace, attribute, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
            String v = getAttributeValue(namespace, attribute);
            if (v == null) return defaultValue;
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public int getAttributeListValue(int index, String[] options, int defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
            String v = getAttributeValue(index);
            return v != null ? Boolean.parseBoolean(v) : defaultValue;
        }

        @Override
        public int getAttributeResourceValue(int index, int defaultValue) {
            return defaultValue;
        }

        @Override
        public int getAttributeIntValue(int index, int defaultValue) {
            String v = getAttributeValue(index);
            if (v == null) return defaultValue;
            try {
                return Integer.decode(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public int getAttributeUnsignedIntValue(int index, int defaultValue) {
            return getAttributeIntValue(index, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(int index, float defaultValue) {
            String v = getAttributeValue(index);
            if (v == null) return defaultValue;
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public String getIdAttribute() {
            return getAttributeValue(null, "id");
        }

        @Override
        public String getClassAttribute() {
            return getAttributeValue(null, "class");
        }

        @Override
        public int getIdAttributeResourceValue(int defaultValue) {
            return defaultValue;
        }

        @Override
        public int getStyleAttribute() {
            return 0;
        }
    }
}
