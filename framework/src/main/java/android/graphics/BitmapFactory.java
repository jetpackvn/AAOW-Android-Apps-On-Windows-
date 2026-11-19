package android.graphics;

import android.content.res.Resources;
import android.util.TypedValue;
import dev.aaow.rt.jni.WebpHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

/** Giải mã ảnh PNG/JPEG/GIF/BMP/WebP sang Bitmap Java2D (hỗ trợ đầy đủ Options cho Glide/Picasso/Coil). */
public class BitmapFactory {
    public static class Options {
        public Bitmap inBitmap;
        public boolean inMutable;
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inPremultiplied = true;
        public boolean inDither;
        public int inDensity;
        public int inTargetDensity;
        public int inScreenDensity;
        public boolean inScaled = true;
        public boolean inPurgeable;
        public boolean inInputShareable;
        public boolean inPreferQualityOverSpeed;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
        public Bitmap.Config outConfig;
        public ColorSpace inPreferredColorSpace;
        public ColorSpace outColorSpace;
        public byte[] inTempStorage;
        public boolean mCancel;

        public void requestCancelDecode() {
            mCancel = true;
        }
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
            byte[] buf = (opts != null && opts.inTempStorage != null && opts.inTempStorage.length >= 1024)
                    ? opts.inTempStorage
                    : new byte[8192];
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

    public static Bitmap decodeResourceStream(Resources res, TypedValue value,
                                              InputStream is, Rect pad, Options opts) {
        if (opts == null) {
            opts = new Options();
        }
        if (opts.inDensity == 0 && value != null) {
            int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = 160;
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;
            }
        }
        if (opts.inTargetDensity == 0 && res != null && res.getDisplayMetrics() != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
        }
        return decodeStream(is, pad, opts);
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
            int sample = Math.max(1, opts.inSampleSize);
            opts.outWidth = Math.max(1, bmp.getWidth() / sample);
            opts.outHeight = Math.max(1, bmp.getHeight() / sample);
            opts.outMimeType = "image/png";
            opts.outConfig = opts.inPreferredConfig != null ? opts.inPreferredConfig : Bitmap.Config.ARGB_8888;
            opts.outColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            if (opts.inJustDecodeBounds) return null;
        }
        return bmp;
    }
}
