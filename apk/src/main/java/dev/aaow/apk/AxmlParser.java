package dev.aaow.apk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Giải mã XML nhị phân của Android (AXML): AndroidManifest.xml, res/layout/*.xml...
 */
public final class AxmlParser {
    private static final int RES_STRING_POOL_TYPE = 0x0001;
    private static final int RES_XML_TYPE = 0x0003;
    private static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
    private static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
    private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
    private static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
    private static final int RES_XML_CDATA_TYPE = 0x0104;
    private static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

    private AxmlParser() {
    }

    public static XmlElement parse(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int type = Short.toUnsignedInt(buf.getShort(0));
        if (type != RES_XML_TYPE) {
            throw new IllegalArgumentException("Không phải XML nhị phân của Android (type=0x" + Integer.toHexString(type) + ")");
        }
        int fileHeaderSize = Short.toUnsignedInt(buf.getShort(2));
        int fileSize = Math.min(buf.getInt(4), data.length);

        StringPool strings = null;
        int[] resourceMap = new int[0];
        Deque<XmlElement> stack = new ArrayDeque<>();
        XmlElement root = null;

        int p = fileHeaderSize;
        while (p + 8 <= fileSize) {
            int chunkType = Short.toUnsignedInt(buf.getShort(p));
            int headerSize = Short.toUnsignedInt(buf.getShort(p + 2));
            int chunkSize = buf.getInt(p + 4);
            if (chunkSize <= 0) break;

            switch (chunkType) {
                case RES_STRING_POOL_TYPE -> strings = StringPool.read(buf, p);
                case RES_XML_RESOURCE_MAP_TYPE -> {
                    int n = (chunkSize - headerSize) / 4;
                    resourceMap = new int[n];
                    for (int i = 0; i < n; i++) resourceMap[i] = buf.getInt(p + headerSize + i * 4);
                }
                case RES_XML_START_ELEMENT_TYPE -> {
                    int line = buf.getInt(p + 8);
                    int ext = p + headerSize;
                    String ns = str(strings, buf.getInt(ext));
                    String name = str(strings, buf.getInt(ext + 4));
                    int attrStart = Short.toUnsignedInt(buf.getShort(ext + 8));
                    int attrSize = Short.toUnsignedInt(buf.getShort(ext + 10));
                    int attrCount = Short.toUnsignedInt(buf.getShort(ext + 12));

                    XmlElement el = new XmlElement(ns, name, line);
                    for (int i = 0; i < attrCount; i++) {
                        int a = ext + attrStart + i * attrSize;
                        int nameIdx = buf.getInt(a + 4);
                        String aNs = str(strings, buf.getInt(a));
                        String aName = str(strings, nameIdx);
                        int rawIdx = buf.getInt(a + 8);
                        int vType = buf.get(a + 15) & 0xff;
                        int vData = buf.getInt(a + 16);
                        String s = vType == ResValue.TYPE_STRING ? str(strings, vData) : str(strings, rawIdx);
                        int resId = nameIdx >= 0 && nameIdx < resourceMap.length ? resourceMap[nameIdx] : 0;
                        el.attributes.add(new XmlElement.Attribute(aNs, aName, resId, new ResValue(vType, vData, s)));
                    }
                    if (stack.isEmpty()) {
                        if (root == null) root = el;
                    } else {
                        stack.peek().children.add(el);
                    }
                    stack.push(el);
                }
                case RES_XML_END_ELEMENT_TYPE -> {
                    if (!stack.isEmpty()) stack.pop();
                }
                case RES_XML_CDATA_TYPE -> {
                    if (!stack.isEmpty()) {
                        String t = str(strings, buf.getInt(p + headerSize));
                        XmlElement top = stack.peek();
                        top.text = top.text == null ? t : top.text + t;
                    }
                }
                case RES_XML_START_NAMESPACE_TYPE, RES_XML_END_NAMESPACE_TYPE -> {
                    // Namespace đã được gắn trực tiếp vào từng thuộc tính, không cần xử lý thêm.
                }
                default -> {
                    // Bỏ qua chunk không biết.
                }
            }
            p += chunkSize;
        }
        if (root == null) throw new IllegalArgumentException("AXML rỗng");
        return root;
    }

    private static String str(StringPool pool, int idx) {
        return pool == null || idx < 0 ? null : pool.get(idx);
    }
}
