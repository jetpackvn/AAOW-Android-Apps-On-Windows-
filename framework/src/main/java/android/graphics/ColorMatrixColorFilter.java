package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter {
    private final ColorMatrix matrix = new ColorMatrix();

    public ColorMatrixColorFilter(ColorMatrix matrix) {
        this.matrix.set(matrix);
    }

    public ColorMatrixColorFilter(float[] array) {
        this.matrix.set(array);
    }

    public void getColorMatrix(ColorMatrix colorMatrix) {
        if (colorMatrix != null) colorMatrix.set(matrix);
    }
}
