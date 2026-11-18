package dev.aaow.apk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Đọc chunk RES_STRING_POOL_TYPE (dùng chung cho AXML và resources.arsc). */
public final class StringPool {
    public static final int UTF8_FLAG = 1 << 8;

    private final String[] strings;

    private StringPool(String[] strings) {
        this.strings = strings;
    }

    public static StringPool read(ByteBuffer buf, int chunkStart) {
        int headerSize = Short.toUnsignedInt(buf.getShort(chunkStart + 2));
        int count = buf.getInt(chunkStart + 8);
        int flags = buf.getInt(chunkStart + 16);
        int stringsStart = buf.getInt(chunkStart + 20);
        boolean utf8 = (flags & UTF8_FLAG) != 0;

        String[] out = new String[count];
        int offsetsBase = chunkStart + headerSize;
        for (int i = 0; i < count; i++) {
            int p = chunkStart + stringsStart + buf.getInt(offsetsBase + i * 4);
            out[i] = utf8 ? readUtf8(buf, p) : readUtf16(buf, p);
        }
        return new StringPool(out);
    }

    private static String readUtf8(ByteBuffer buf, int p) {
        // Độ dài theo UTF-16 (bỏ qua), sau đó là độ dài theo byte.
        int b = buf.get(p++) & 0xff;
        if ((b & 0x80) != 0) p++;
        int len = buf.get(p++) & 0xff;
        if ((len & 0x80) != 0) {
            len = ((len & 0x7f) << 8) | (buf.get(p++) & 0xff);
        }
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) bytes[i] = buf.get(p + i);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String readUtf16(ByteBuffer buf, int p) {
        int len = Short.toUnsignedInt(buf.getShort(p));
        p += 2;
        if ((len & 0x8000) != 0) {
            len = ((len & 0x7fff) << 16) | Short.toUnsignedInt(buf.getShort(p));
            p += 2;
        }
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) chars[i] = buf.getChar(p + i * 2);
        return new String(chars);
    }

    public String get(int index) {
        return index >= 0 && index < strings.length ? strings[index] : null;
    }

    public int size() {
        return strings.length;
    }
}
