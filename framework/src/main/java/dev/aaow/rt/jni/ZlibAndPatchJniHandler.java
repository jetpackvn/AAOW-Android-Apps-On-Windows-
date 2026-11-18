package dev.aaow.rt.jni;

import dev.aaow.rt.NativeBridge;

import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Bộ giả lập JNI cho thư viện nén zlib / libdeflater7z / HPatch.
 */
public final class ZlibAndPatchJniHandler implements NativeJniHandler {
    @Override
    public boolean supports(String className) {
        return className.startsWith("com.tencent.yybsdk.apkpatch.")
                || "java.util.zip.CRC32".equals(className)
                || "java.util.zip.Deflater".equals(className)
                || "java.util.zip.Inflater".equals(className);
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args) {
        if ("com.tencent.yybsdk.apkpatch.patch64.DeflateTask".equals(className)
                && "deflateByCallback".equals(methodName)) {
            if (args != null && args.length >= 2 && args[1] instanceof byte[] input) {
                Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
                deflater.setInput(input);
                deflater.finish();
                ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
                byte[] buf = new byte[1024];
                while (!deflater.finished()) {
                    int n = deflater.deflate(buf);
                    if (n <= 0) break;
                    out.write(buf, 0, n);
                }
                deflater.end();
                return out.size();
            }
            return 0;
        }
        if ("com.tencent.yybsdk.apkpatch.hdiff.HPatch".equals(className)
                && "patchWithCallBack".equals(methodName)) {
            return -1;
        }
        if ("java.util.zip.CRC32".equals(className) && "updateBytes".equals(methodName)
                && args != null && args.length >= 4 && args[1] instanceof byte[] b) {
            CRC32 crc = new CRC32();
            int off = ((Number) args[2]).intValue();
            int len = ((Number) args[3]).intValue();
            crc.update(b, off, len);
            return (int) crc.getValue();
        }
        return NativeBridge.UNHANDLED;
    }

    public static byte[] compressZlib(byte[] input) {
        if (input == null) return new byte[0];
        Deflater d = new Deflater();
        d.setInput(input);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        byte[] buf = new byte[1024];
        while (!d.finished()) {
            int n = d.deflate(buf);
            if (n <= 0) break;
            out.write(buf, 0, n);
        }
        d.end();
        return out.toByteArray();
    }

    public static byte[] decompressZlib(byte[] input) {
        if (input == null) return new byte[0];
        Inflater inf = new Inflater();
        inf.setInput(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length * 2);
        byte[] buf = new byte[1024];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n <= 0) break;
                out.write(buf, 0, n);
            }
        } catch (Exception ignored) {
        } finally {
            inf.end();
        }
        return out.toByteArray();
    }
}
