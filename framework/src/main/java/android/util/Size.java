package android.util;

/** Kích thước nguyên bất biến (width x height) dùng bởi ImageDecoder, MediaStore, Camera2. */
public final class Size {
    private final int width;
    private final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static Size parseSize(String string) throws NumberFormatException {
        if (string == null) {
            throw new NullPointerException("string must not be null");
        }
        int sep = string.indexOf('*');
        if (sep < 0) {
            sep = string.indexOf('x');
        }
        if (sep < 0) {
            throw new NumberFormatException("Invalid Size: \"" + string + "\"");
        }
        try {
            return new Size(Integer.parseInt(string.substring(0, sep)),
                    Integer.parseInt(string.substring(sep + 1)));
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid Size: \"" + string + "\"");
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Size other && width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return height ^ ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
