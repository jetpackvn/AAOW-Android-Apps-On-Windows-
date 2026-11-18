package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class Bitmap implements Parcelable {
    public enum Config {
        ALPHA_8,
        RGB_565,
        ARGB_4444,
        ARGB_8888,
        RGBA_F16,
        HARDWARE
    }

    private final BufferedImage image;

    public Bitmap(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getAwtImage() {
        return image;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB));
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        BufferedImage dst = new BufferedImage(Math.max(1, dstWidth), Math.max(1, dstHeight), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src.image, 0, 0, dstWidth, dstHeight, null);
        g.dispose();
        return new Bitmap(dst);
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getPixel(int x, int y) {
        return image.getRGB(x, y);
    }

    public void setPixel(int x, int y, int color) {
        image.setRGB(x, y, color);
    }

    public void eraseColor(int c) {
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.toAwt(c));
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    public void recycle() {
    }

    public boolean isRecycled() {
        return false;
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
