package libcore.io;

import java.nio.ByteOrder;

/** Cài đặt libcore.io.Memory hỗ trợ đọc/ghi bộ nhớ byte[]/số nguyên cho các thư viện Android/Okio/ProtocolBuffers. */
public final class Memory {
    private Memory() {
    }

    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return ((src[offset] & 0xFF) << 24)
                    | ((src[offset + 1] & 0xFF) << 16)
                    | ((src[offset + 2] & 0xFF) << 8)
                    | (src[offset + 3] & 0xFF);
        }
        return (src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24);
    }

    public static long peekLong(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            int h = peekInt(src, offset, order);
            int l = peekInt(src, offset + 4, order);
            return ((h & 0xFFFFFFFFL) << 32) | (l & 0xFFFFFFFFL);
        }
        int l = peekInt(src, offset, order);
        int h = peekInt(src, offset + 4, order);
        return ((h & 0xFFFFFFFFL) << 32) | (l & 0xFFFFFFFFL);
    }

    public static short peekShort(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short) (((src[offset] & 0xFF) << 8) | (src[offset + 1] & 0xFF));
        }
        return (short) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8));
    }

    public static void pokeInt(byte[] dst, int offset, int value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            dst[offset] = (byte) (value >>> 24);
            dst[offset + 1] = (byte) (value >>> 16);
            dst[offset + 2] = (byte) (value >>> 8);
            dst[offset + 3] = (byte) value;
        } else {
            dst[offset] = (byte) value;
            dst[offset + 1] = (byte) (value >>> 8);
            dst[offset + 2] = (byte) (value >>> 16);
            dst[offset + 3] = (byte) (value >>> 24);
        }
    }

    public static void pokeLong(byte[] dst, int offset, long value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            pokeInt(dst, offset, (int) (value >>> 32), order);
            pokeInt(dst, offset + 4, (int) value, order);
        } else {
            pokeInt(dst, offset, (int) value, order);
            pokeInt(dst, offset + 4, (int) (value >>> 32), order);
        }
    }

    public static void pokeShort(byte[] dst, int offset, short value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            dst[offset] = (byte) (value >>> 8);
            dst[offset + 1] = (byte) value;
        } else {
            dst[offset] = (byte) value;
            dst[offset + 1] = (byte) (value >>> 8);
        }
    }
}
