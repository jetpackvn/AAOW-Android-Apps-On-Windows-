package android.util;

public class TypedValue {
    public static final int TYPE_NULL = 0x00;
    public static final int TYPE_REFERENCE = 0x01;
    public static final int TYPE_ATTRIBUTE = 0x02;
    public static final int TYPE_STRING = 0x03;
    public static final int TYPE_FLOAT = 0x04;
    public static final int TYPE_DIMENSION = 0x05;
    public static final int TYPE_FRACTION = 0x06;
    public static final int TYPE_FIRST_INT = 0x10;
    public static final int TYPE_INT_DEC = 0x10;
    public static final int TYPE_INT_HEX = 0x11;
    public static final int TYPE_INT_BOOLEAN = 0x12;
    public static final int TYPE_FIRST_COLOR_INT = 0x1c;
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;
    public static final int TYPE_LAST_COLOR_INT = 0x1f;
    public static final int TYPE_LAST_INT = 0x1f;

    public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    public static final int COMPLEX_UNIT_PT = 3;
    public static final int COMPLEX_UNIT_IN = 4;
    public static final int COMPLEX_UNIT_MM = 5;
    public static final int COMPLEX_UNIT_SHIFT = 0;
    public static final int COMPLEX_UNIT_MASK = 0xf;
    public static final int COMPLEX_UNIT_FRACTION = 0;
    public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_NONE = 0xffff;

    public int type;
    public CharSequence string;
    public int data;
    public int assetCookie;
    public int resourceId;
    public int changingConfigurations;
    public int density;

    public final float getFloat() {
        return Float.intBitsToFloat(data);
    }

    public static float applyDimension(int unit, float value, DisplayMetrics metrics) {
        return switch (unit) {
            case COMPLEX_UNIT_PX -> value;
            case COMPLEX_UNIT_DIP -> value * metrics.density;
            case COMPLEX_UNIT_SP -> value * metrics.scaledDensity;
            case COMPLEX_UNIT_PT -> value * metrics.xdpi * (1.0f / 72);
            case COMPLEX_UNIT_IN -> value * metrics.xdpi;
            case COMPLEX_UNIT_MM -> value * metrics.xdpi * (1.0f / 25.4f);
            default -> 0;
        };
    }

    public float getDimension(DisplayMetrics metrics) {
        return complexToDimension(data, metrics);
    }

    public static float complexToDimension(int data, DisplayMetrics metrics) {
        return applyDimension(data & 0xf, complexToFloat(data), metrics);
    }

    public static int complexToDimensionPixelSize(int data, DisplayMetrics metrics) {
        final float value = complexToFloat(data);
        final float f = applyDimension(data & 0xf, value, metrics);
        final int res = (int) ((f >= 0) ? (f + 0.5f) : (f - 0.5f));
        if (res != 0) return res;
        if (value == 0) return 0;
        return value > 0 ? 1 : -1;
    }

    public static int complexToDimensionPixelOffset(int data, DisplayMetrics metrics) {
        return (int) applyDimension(data & 0xf, complexToFloat(data), metrics);
    }

    public static float complexToFloat(int complex) {
        float mantissaMult = 1.0f / (1 << 8);
        float[] radixMults = {
                1.0f * mantissaMult, 1.0f / (1 << 7) * mantissaMult,
                1.0f / (1 << 15) * mantissaMult, 1.0f / (1 << 23) * mantissaMult
        };
        return (complex & 0xffffff00) * radixMults[(complex >> 4) & 3];
    }

    public float getFraction(float base, float pbase) {
        return complexToFraction(data, base, pbase);
    }

    public static float complexToFraction(int data, float base, float pbase) {
        return switch ((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK) {
            case COMPLEX_UNIT_FRACTION -> complexToFloat(data) * base;
            case COMPLEX_UNIT_FRACTION_PARENT -> complexToFloat(data) * pbase;
            default -> 0f;
        };
    }
}
