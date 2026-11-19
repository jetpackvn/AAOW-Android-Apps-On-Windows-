package android.util;

/** Kích thước dấu phẩy động bất biến (width x height). */
public final class SizeF {
    private final float width;
    private final float height;

    public SizeF(float width, float height) {
        if (Float.isNaN(width) || Float.isNaN(height)) {
            throw new IllegalArgumentException("Width and height must not be NaN");
        }
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public static SizeF parseSizeF(String string) throws NumberFormatException {
        if (string == null) {
            throw new NullPointerException("string must not be null");
        }
        int sep = string.indexOf('*');
        if (sep < 0) {
            sep = string.indexOf('x');
        }
        if (sep < 0) {
            throw new NumberFormatException("Invalid SizeF: \"" + string + "\"");
        }
        try {
            return new SizeF(Float.parseFloat(string.substring(0, sep)),
                    Float.parseFloat(string.substring(sep + 1)));
        } catch (IllegalArgumentException e) {
            throw new NumberFormatException("Invalid SizeF: \"" + string + "\"");
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SizeF other && width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(width) ^ Float.floatToIntBits(height);
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
