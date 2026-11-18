package android.graphics;

import android.content.res.Resources;
import dev.aaow.rt.jni.WebpHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

/** Giải mã ảnh PNG/JPEG/GIF/BMP/WebP sang Bitmap Java2D. */
public class BitmapFactory {
    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inMutable;
        public boolean inScaled = true;
        public int inDensity;
        public int inTargetDensity;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        return WebpHelper.decodeWithOptions(data, offset, length, opts);
    }

    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        if (is == null) return null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            byte[] bytes = bos.toByteArray();
            return decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    public static Bitmap decodeFile(String pathName, Options opts) {
        if (pathName == null) return null;
        try {
            byte[] bytes = Files.readAllBytes(new File(pathName).toPath());
            return decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect outPadding, Options opts) {
        if (fd == null) return null;
        try (FileInputStream fis = new FileInputStream(fd)) {
            return decodeStream(fis, outPadding, opts);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        if (res == null) return null;
        Bitmap bmp = res.loadBitmap(id);
        if (bmp != null && opts != null) {
            opts.outWidth = bmp.getWidth();
            opts.outHeight = bmp.getHeight();
            if (opts.inJustDecodeBounds) return null;
        }
        return bmp;
    }
}
