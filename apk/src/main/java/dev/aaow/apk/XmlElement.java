package dev.aaow.apk;

import java.util.ArrayList;
import java.util.List;

/** Một phần tử XML đã giải mã từ định dạng AXML nhị phân. */
public final class XmlElement {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String AUTO_NS = "http://schemas.android.com/apk/res-auto";

    public final String namespace;
    public final String name;
    public final int line;
    public final List<Attribute> attributes = new ArrayList<>();
    public final List<XmlElement> children = new ArrayList<>();
    public String text;

    public XmlElement(String namespace, String name, int line) {
        this.namespace = namespace;
        this.name = name;
        this.line = line;
    }

    /** Thuộc tính trong namespace android: (hoặc không namespace nếu không tìm thấy). */
    public Attribute attr(String name) {
        Attribute a = attr(ANDROID_NS, name);
        return a != null ? a : attr(null, name);
    }

    public Attribute attr(String ns, String name) {
        for (Attribute a : attributes) {
            if (name.equals(a.name) && (ns == null ? a.namespace == null || a.namespace.isEmpty() : ns.equals(a.namespace))) {
                return a;
            }
        }
        return null;
    }

    public List<XmlElement> children(String name) {
        List<XmlElement> out = new ArrayList<>();
        for (XmlElement c : children) if (name.equals(c.name)) out.add(c);
        return out;
    }

    /** In lại cây XML để debug. */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        dump(sb, 0);
        return sb.toString();
    }

    private void dump(StringBuilder sb, int depth) {
        String ind = "  ".repeat(depth);
        sb.append(ind).append('<').append(name);
        for (Attribute a : attributes) {
            sb.append(' ');
            if (ANDROID_NS.equals(a.namespace)) sb.append("android:");
            else if (a.namespace != null && !a.namespace.isEmpty()) sb.append("app:");
            sb.append(a.name).append("=\"").append(a.value.coerceToString()).append('"');
        }
        if (children.isEmpty()) {
            sb.append("/>\n");
            return;
        }
        sb.append(">\n");
        for (XmlElement c : children) c.dump(sb, depth + 1);
        sb.append(ind).append("</").append(name).append(">\n");
    }

    public static final class Attribute {
        public final String namespace;
        public final String name;
        /** ID resource của thuộc tính (ví dụ android.R.attr.text), 0 nếu không có. */
        public final int resourceId;
        public final ResValue value;

        public Attribute(String namespace, String name, int resourceId, ResValue value) {
            this.namespace = namespace;
            this.name = name;
            this.resourceId = resourceId;
            this.value = value;
        }

        @Override
        public String toString() {
            return name + "=" + value.coerceToString();
        }
    }
}
