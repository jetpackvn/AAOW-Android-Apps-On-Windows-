package dev.aaow.rt.jni;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import dev.aaow.rt.NativeBridge;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Bộ giả lập JNI cho Glide WebP Decoder (libglide-webp.so).
 */
public final class WebpJniHandler implements NativeJniHandler {
    @Override
    public boolean supports(String className) {
        return className.startsWith("com.bumptech.glide.integration.webp.");
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args) {
        if ("com.bumptech.glide.integration.webp.WebpBitmapFactory".equals(className)) {
            if ("nativeDecodeByteArray".equals(methodName) && args != null && args.length >= 4) {
                byte[] data = (byte[]) args[0];
                int off = args[1] instanceof Number n ? n.intValue() : 0;
                int len = args[2] instanceof Number n ? n.intValue() : (data != null ? data.length : 0);
                BitmapFactory.Options opts = args[3] instanceof BitmapFactory.Options o ? o : null;
                return WebpHelper.decodeWithOptions(data, off, len, opts);
            }
            if ("nativeDecodeStream".equals(methodName) && args != null && args.length >= 2) {
                InputStream is = args[0] instanceof InputStream s ? s : null;
                BitmapFactory.Options opts = args[1] instanceof BitmapFactory.Options o ? o : null;
                if (is == null) return null;
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) {
                        bos.write(buf, 0, r);
                    }
                    byte[] bytes = bos.toByteArray();
                    return WebpHelper.decodeWithOptions(bytes, 0, bytes.length, opts);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        if ("com.bumptech.glide.integration.webp.WebpImage".equals(className)) {
            return switch (methodName) {
                case "nativeCreateFromDirectByteBuffer" -> {
                    if (args != null && args.length >= 1 && args[0] instanceof ByteBuffer bb) {
                        ByteBuffer dup = bb.duplicate();
                        byte[] raw = new byte[dup.remaining()];
                        dup.get(raw);
                        WebpHelper.ImageHeader hdr = WebpHelper.parseWebpHeader(raw, 0, raw.length);
                        if (hdr != null) {
                            try {
                                var ctor = Class.forName(className, false, bb.getClass().getClassLoader());
                                yield null; // Để fallback an toàn nếu constructor nội bộ khác signature
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    yield null;
                }
                case "nativeGetSizeInBytes" -> 4096;
                case "nativeDispose", "nativeFinalize" -> null;
                default -> NativeBridge.UNHANDLED;
            };
        }
        if ("com.bumptech.glide.integration.webp.WebpFrame".equals(className)) {
            return switch (methodName) {
                case "nativeRenderFrame" -> {
                    if (args != null && args.length >= 3 && args[2] instanceof Bitmap bmp) {
                        bmp.eraseColor(0);
                    }
                    yield null;
                }
                case "nativeDispose", "nativeFinalize" -> null;
                default -> NativeBridge.UNHANDLED;
            };
        }
        return NativeBridge.UNHANDLED;
    }
}
