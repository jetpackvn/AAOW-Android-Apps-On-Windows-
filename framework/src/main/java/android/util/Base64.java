package android.util;

import java.nio.charset.StandardCharsets;

/** Mã hóa & giải mã Base64 tương thích đầy đủ các cờ của Android (DEFAULT, NO_WRAP, URL_SAFE, NO_PADDING...). */
public class Base64 {
    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;
    public static final int NO_CLOSE = 16;

    private Base64() {
    }

    public static byte[] decode(String str, int flags) {
        if (str == null) throw new IllegalArgumentException("Input string is null");
        return decode(str.getBytes(StandardCharsets.US_ASCII), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        if (input == null) throw new IllegalArgumentException("Input is null");
        return decode(input, 0, input.length, flags);
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        if (input == null) throw new IllegalArgumentException("Input is null");
        byte[] slice = new byte[len];
        System.arraycopy(input, offset, slice, 0, len);
        String raw = new String(slice, StandardCharsets.US_ASCII).replaceAll("\\s+", "");
        if ((flags & URL_SAFE) != 0) {
            return java.util.Base64.getUrlDecoder().decode(raw);
        }
        return java.util.Base64.getDecoder().decode(raw);
    }

    public static byte[] encode(byte[] input, int flags) {
        if (input == null) throw new IllegalArgumentException("Input is null");
        return encode(input, 0, input.length, flags);
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        return encodeToString(input, offset, len, flags).getBytes(StandardCharsets.US_ASCII);
    }

    public static String encodeToString(byte[] input, int flags) {
        if (input == null) throw new IllegalArgumentException("Input is null");
        return encodeToString(input, 0, input.length, flags);
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        if (input == null) throw new IllegalArgumentException("Input is null");
        byte[] slice;
        if (offset == 0 && len == input.length) {
            slice = input;
        } else {
            slice = new byte[len];
            System.arraycopy(input, offset, slice, 0, len);
        }
        java.util.Base64.Encoder encoder;
        if ((flags & NO_WRAP) != 0) {
            encoder = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();
        } else {
            byte[] sep = (flags & CRLF) != 0 ? new byte[]{'\r', '\n'} : new byte[]{'\n'};
            encoder = java.util.Base64.getMimeEncoder(76, sep);
        }
        if ((flags & NO_PADDING) != 0) {
            encoder = encoder.withoutPadding();
        }
        String out = encoder.encodeToString(slice);
        if ((flags & URL_SAFE) != 0 && (flags & NO_WRAP) == 0) {
            out = out.replace('+', '-').replace('/', '_');
        }
        if ((flags & NO_WRAP) == 0 && !out.isEmpty()) {
            out += ((flags & CRLF) != 0) ? "\r\n" : "\n";
        }
        return out;
    }
}
