package android.view;

import android.graphics.Point;
import android.util.DisplayMetrics;

public class Display {
    public static final int DEFAULT_DISPLAY = 0;

    private final DisplayMetrics metrics;

    public Display(DisplayMetrics metrics) {
        this.metrics = metrics != null ? metrics : new DisplayMetrics();
    }

    public int getDisplayId() {
        return DEFAULT_DISPLAY;
    }

    public int getRotation() {
        return Surface.ROTATION_0;
    }

    public void getMetrics(DisplayMetrics outMetrics) {
        if (outMetrics == null) return;
        outMetrics.widthPixels = metrics.widthPixels;
        outMetrics.heightPixels = metrics.heightPixels;
        outMetrics.density = metrics.density;
        outMetrics.densityDpi = metrics.densityDpi;
        outMetrics.scaledDensity = metrics.scaledDensity;
        outMetrics.xdpi = metrics.xdpi;
        outMetrics.ydpi = metrics.ydpi;
    }

    public void getRealMetrics(DisplayMetrics outMetrics) {
        getMetrics(outMetrics);
    }

    public void getSize(Point outSize) {
        if (outSize != null) {
            outSize.x = metrics.widthPixels;
            outSize.y = metrics.heightPixels;
        }
    }

    public void getRealSize(Point outSize) {
        getSize(outSize);
    }

    public int getWidth() {
        return metrics.widthPixels;
    }

    public int getHeight() {
        return metrics.heightPixels;
    }
}
