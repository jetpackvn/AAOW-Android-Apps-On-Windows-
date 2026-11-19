package android.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Luồng giải mã Base64 tương thích {@code android.util.Base64InputStream}.
 */
public class Base64InputStream extends FilterInputStream {
    private final int flags;
    private byte[] decoded;
    private int pos;
    private boolean closed;

    public Base64InputStream(InputStream in, int flags) {
        super(in);
        this.flags = flags;
    }

    private void ensureDecoded() throws IOException {
        if (decoded != null) {
            return;
        }
        if (closed) {
            throw new IOException("Stream closed");
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            raw.write(buf, 0, n);
        }
        byte[] inputBytes = raw.toByteArray();
        if (inputBytes.length == 0) {
            decoded = new byte[0];
        } else {
            try {
                decoded = Base64.decode(inputBytes, flags);
            } catch (IllegalArgumentException e) {
                throw new IOException("Bad Base64 input", e);
            }
        }
        pos = 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available() {
        if (decoded != null) {
            return Math.max(0, decoded.length - pos);
        }
        return 0;
    }

    @Override
    public int read() throws IOException {
        ensureDecoded();
        if (pos >= decoded.length) {
            return -1;
        }
        return decoded[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        ensureDecoded();
        if (pos >= decoded.length) {
            return -1;
        }
        int count = Math.min(len, decoded.length - pos);
        System.arraycopy(decoded, pos, b, off, count);
        pos += count;
        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        ensureDecoded();
        long skipped = Math.min(n, (long) (decoded.length - pos));
        pos += (int) skipped;
        return skipped;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if ((flags & Base64.NO_CLOSE) == 0 && in != null) {
                in.close();
            }
        }
    }
}
