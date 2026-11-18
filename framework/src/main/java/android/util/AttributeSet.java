package android.util;

import dev.aaow.apk.ResValue;
import dev.aaow.apk.ResourceTable;
import dev.aaow.apk.XmlElement;

/** Giao diện AttributeSet chuẩn của Android + cài đặt dựa trên XmlElement đã giải mã từ AXML. */
public interface AttributeSet {
    int getAttributeCount();
    String getAttributeNamespace(int index);
    String getAttributeName(int index);
    String getAttributeValue(int index);
    String getAttributeValue(String namespace, String name);
    String getPositionDescription();
    int getAttributeNameResource(int index);
    int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue);
    boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue);
    int getAttributeResourceValue(String namespace, String attribute, int defaultValue);
    int getAttributeIntValue(String namespace, String attribute, int defaultValue);
    int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue);
    float getAttributeFloatValue(String namespace, String attribute, float defaultValue);
    int getAttributeListValue(int index, String[] options, int defaultValue);
    boolean getAttributeBooleanValue(int index, boolean defaultValue);
    int getAttributeResourceValue(int index, int defaultValue);
    int getAttributeIntValue(int index, int defaultValue);
    int getAttributeUnsignedIntValue(int index, int defaultValue);
    float getAttributeFloatValue(int index, float defaultValue);
    String getIdAttribute();
    String getClassAttribute();
    int getIdAttributeResourceValue(int defaultValue);
    int getStyleAttribute();

    /** Cài đặt AttributeSet bọc quanh một XmlElement từ file layout AXML. */
    final class XmlAttributeSet implements AttributeSet {
        public final XmlElement element;
        public final ResourceTable resources;

        public XmlAttributeSet(XmlElement element, ResourceTable resources) {
            this.element = element;
            this.resources = resources;
        }

        @Override
        public int getAttributeCount() {
            return element.attributes.size();
        }

        @Override
        public String getAttributeNamespace(int index) {
            return element.attributes.get(index).namespace;
        }

        @Override
        public String getAttributeName(int index) {
            return element.attributes.get(index).name;
        }

        @Override
        public String getAttributeValue(int index) {
            ResValue v = resources.resolve(element.attributes.get(index).value);
            return v == null ? null : v.coerceToString();
        }

        @Override
        public String getAttributeValue(String namespace, String name) {
            XmlElement.Attribute a = element.attr(namespace, name);
            if (a == null) return null;
            ResValue v = resources.resolve(a.value);
            return v == null ? null : v.coerceToString();
        }

        @Override
        public String getPositionDescription() {
            return "<" + element.name + " line=" + element.line + ">";
        }

        @Override
        public int getAttributeNameResource(int index) {
            return element.attributes.get(index).resourceId;
        }

        @Override
        public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
            XmlElement.Attribute a = element.attr(namespace, attribute);
            if (a == null) return defaultValue;
            ResValue v = resources.resolve(a.value);
            return v != null ? v.data() != 0 : defaultValue;
        }

        @Override
        public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
            XmlElement.Attribute a = element.attr(namespace, attribute);
            return a != null && a.value.isReference() ? a.value.data() : defaultValue;
        }

        @Override
        public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
            XmlElement.Attribute a = element.attr(namespace, attribute);
            if (a == null) return defaultValue;
            ResValue v = resources.resolve(a.value);
            return v != null && v.isInt() ? v.data() : defaultValue;
        }

        @Override
        public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
            return getAttributeIntValue(namespace, attribute, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
            XmlElement.Attribute a = element.attr(namespace, attribute);
            if (a == null) return defaultValue;
            ResValue v = resources.resolve(a.value);
            return v != null ? v.getFloat() : defaultValue;
        }

        @Override
        public int getAttributeListValue(int index, String[] options, int defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
            ResValue v = resources.resolve(element.attributes.get(index).value);
            return v != null ? v.data() != 0 : defaultValue;
        }

        @Override
        public int getAttributeResourceValue(int index, int defaultValue) {
            ResValue v = element.attributes.get(index).value;
            return v.isReference() ? v.data() : defaultValue;
        }

        @Override
        public int getAttributeIntValue(int index, int defaultValue) {
            ResValue v = resources.resolve(element.attributes.get(index).value);
            return v != null && v.isInt() ? v.data() : defaultValue;
        }

        @Override
        public int getAttributeUnsignedIntValue(int index, int defaultValue) {
            return getAttributeIntValue(index, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(int index, float defaultValue) {
            ResValue v = resources.resolve(element.attributes.get(index).value);
            return v != null ? v.getFloat() : defaultValue;
        }

        @Override
        public String getIdAttribute() {
            return getAttributeValue(XmlElement.ANDROID_NS, "id");
        }

        @Override
        public String getClassAttribute() {
            return getAttributeValue(null, "class");
        }

        @Override
        public int getIdAttributeResourceValue(int defaultValue) {
            return getAttributeResourceValue(XmlElement.ANDROID_NS, "id", defaultValue);
        }

        @Override
        public int getStyleAttribute() {
            return getAttributeResourceValue(null, "style", 0);
        }
    }
}
