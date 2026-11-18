package android.graphics;

public class BitmapShader extends Shader {
    public static final int FILTER_MODE_DEFAULT = 0;
    public static final int FILTER_MODE_NEAREST = 1;
    public static final int FILTER_MODE_LINEAR = 2;

    private int filterMode = FILTER_MODE_DEFAULT;

    public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
    }

    public int getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(int mode) {
        this.filterMode = mode;
    }
}
