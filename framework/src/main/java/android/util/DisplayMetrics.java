package android.util;

public class DisplayMetrics {
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_XXXHIGH = 640;
    public static final int DENSITY_DEFAULT = DENSITY_MEDIUM;

    public int widthPixels = 412;
    public int heightPixels = 840;
    public float density = 1.0f;
    public int densityDpi = DENSITY_MEDIUM;
    public float scaledDensity = 1.0f;
    public float xdpi = 160f;
    public float ydpi = 160f;

    public void setTo(DisplayMetrics o) {
        if (this == o) return;
        widthPixels = o.widthPixels;
        heightPixels = o.heightPixels;
        density = o.density;
        densityDpi = o.densityDpi;
        scaledDensity = o.scaledDensity;
        xdpi = o.xdpi;
        ydpi = o.ydpi;
    }

    public void setToDefaults() {
        widthPixels = 412;
        heightPixels = 840;
        density = 1.0f;
        densityDpi = DENSITY_MEDIUM;
        scaledDensity = 1.0f;
        xdpi = 160f;
        ydpi = 160f;
    }
}
