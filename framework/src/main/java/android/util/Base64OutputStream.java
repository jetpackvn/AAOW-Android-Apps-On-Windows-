package android.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Luồng mã hóa Base64 tương thích {@code android.util.Base64OutputStream}.
 */
public class Base64OutputStream extends FilterOutputStream {
    private final int flags;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean closed;

    public Base64OutputStream(OutputStream out, int flags) {
        super(out);
        this.flags = flags;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        if (len <= 0) {
            return;
        }
        buffer.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException thrown = null;
        try {
            byte[] raw = buffer.toByteArray();
            if (raw.length > 0) {
                byte[] encoded = Base64.encode(raw, flags);
                out.write(encoded);
            }
            out.flush();
        } catch (IOException e) {
            thrown = e;
        }
        try {
            if ((flags & Base64.NO_CLOSE) == 0) {
                out.close();
            } else {
                out.flush();
            }
        } catch (IOException e) {
            if (thrown == null) {
                thrown = e;
            }
        }
        if (thrown != null) {
            throw thrown;
        }
    }
}
