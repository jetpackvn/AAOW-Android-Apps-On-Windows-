package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

public final class Bitmap implements Parcelable {
    public enum CompressFormat {
        JPEG,
        PNG,
        WEBP,
        WEBP_LOSSY,
        WEBP_LOSSLESS
    }

    public enum Config {
        ALPHA_8,
        RGB_565,
        ARGB_4444,
        ARGB_8888,
        RGBA_F16,
        HARDWARE
    }

    private static final java.util.concurrent.atomic.AtomicLong NEXT_NATIVE_PTR = new java.util.concurrent.atomic.AtomicLong(0x4000_0001L);
    private final long mNativePtr = NEXT_NATIVE_PTR.getAndIncrement();
    private BufferedImage image;
    private final int width;
    private final int height;
    private int density = 480;
    private boolean hasAlpha = true;
    private boolean mutable = true;
    private boolean hasMipMap;
    private volatile boolean recycled;

    public Bitmap(BufferedImage image) {
        this.image = image;
        this.width = image != null ? image.getWidth() : 0;
        this.height = image != null ? image.getHeight() : 0;
        if (image != null && image.getColorModel() != null) {
            this.hasAlpha = image.getColorModel().hasAlpha();
        }
    }

    public BufferedImage getAwtImage() {
        return recycled ? null : image;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB));
    }

    public static Bitmap createBitmap(Bitmap src) {
        if (src == null) return null;
        return createBitmap(src, 0, 0, src.getWidth(), src.getHeight());
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        return createBitmap(source, x, y, width, height, null, false);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter) {
        if (source == null || source.isRecycled() || source.image == null) {
            return createBitmap(width, height, Config.ARGB_8888);
        }
        int sx = Math.max(0, Math.min(source.width - 1, x));
        int sy = Math.max(0, Math.min(source.height - 1, y));
        int sw = Math.max(1, Math.min(source.width - sx, width));
        int sh = Math.max(1, Math.min(source.height - sy, height));
        BufferedImage dst = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        if (filter) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        g.drawImage(source.image, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
        g.dispose();
        return new Bitmap(dst);
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        if (src == null || src.isRecycled() || src.image == null) {
            return createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
        }
        int dw = Math.max(1, dstWidth);
        int dh = Math.max(1, dstHeight);
        BufferedImage dst = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        if (filter) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        g.drawImage(src.image, 0, 0, dw, dh, null);
        g.dispose();
        return new Bitmap(dst);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getByteCount() {
        return width * height * 4;
    }

    public int getAllocationByteCount() {
        return getByteCount();
    }

    public int getRowBytes() {
        return width * 4;
    }

    public boolean isMutable() {
        return mutable && !recycled;
    }

    public boolean hasMipMap() {
        return hasMipMap;
    }

    public void setHasMipMap(boolean hasMipMap) {
        this.hasMipMap = hasMipMap;
    }

    public int getPixel(int x, int y) {
        return (recycled || image == null) ? 0 : image.getRGB(x, y);
    }

    public void setPixel(int x, int y, int color) {
        if (!recycled && image != null) {
            image.setRGB(x, y, color);
        }
    }

    public void eraseColor(int c) {
        if (recycled || image == null) return;
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.toAwt(c));
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    public Config getConfig() {
        return Config.ARGB_8888;
    }

    public int getDensity() {
        return density;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public void setHasAlpha(boolean hasAlpha) {
        this.hasAlpha = hasAlpha;
    }

    public Bitmap copy(Config config, boolean isMutable) {
        if (recycled || image == null) return null;
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        Bitmap b = new Bitmap(copy);
        b.setDensity(density);
        b.setHasAlpha(hasAlpha);
        b.mutable = isMutable;
        return b;
    }

    public boolean compress(CompressFormat format, int quality, OutputStream stream) {
        if (stream == null || recycled || image == null) return false;
        try {
            if (format == CompressFormat.JPEG) {
                BufferedImage rgb = image;
                if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getColorModel().hasAlpha()) {
                    rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                }
                java.util.Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (writers.hasNext()) {
                    javax.imageio.ImageWriter writer = writers.next();
                    try (javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(stream)) {
                        writer.setOutput(ios);
                        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                        if (param.canWriteCompressed()) {
                            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(Math.max(0.05f, Math.min(1.0f, quality / 100f)));
                        }
                        writer.write(null, new javax.imageio.IIOImage(rgb, null, null), param);
                        ios.flush();
                        return true;
                    } finally {
                        writer.dispose();
                    }
                }
                return ImageIO.write(rgb, "jpg", stream);
            }
            return ImageIO.write(image, "png", stream);
        } catch (Exception e) {
            return false;
        }
    }

    public void recycle() {
        if (!recycled) {
            recycled = true;
            if (image != null) {
                image.flush();
                image = null;
            }
        }
    }

    public void prepareToDraw() {
    }

    public void reconfigure(int width, int height, Config config) {
        if (width > 0 && height > 0 && (image == null || image.getWidth() != width || image.getHeight() != height)) {
            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public boolean sameAs(Bitmap other) {
        if (this == other) return true;
        if (other == null || recycled || other.recycled || width != other.width || height != other.height) {
            return false;
        }
        return image != null && other.image != null;
    }

    public boolean isRecycled() {
        return recycled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(getWidth());
        p.writeInt(getHeight());
    }
}
