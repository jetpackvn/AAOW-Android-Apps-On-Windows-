package android.graphics;

import android.content.res.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class BitmapFactory {
    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        if (data == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data, offset, length));
            if (img == null) return null;
            if (opts != null) {
                opts.outWidth = img.getWidth();
                opts.outHeight = img.getHeight();
                if (opts.inJustDecodeBounds) return null;
            }
            return new Bitmap(img);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeStream(InputStream is) {
        if (is == null) return null;
        try {
            BufferedImage img = ImageIO.read(is);
            return img != null ? new Bitmap(img) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeResource(Resources res, int id) {
        if (res == null) return null;
        return res.loadBitmap(id);
    }
}
