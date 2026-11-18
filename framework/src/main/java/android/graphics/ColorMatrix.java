package android.graphics;

import java.util.Arrays;

public class ColorMatrix {
    private final float[] array = new float[20];

    public ColorMatrix() {
        reset();
    }

    public ColorMatrix(float[] src) {
        if (src != null) {
            System.arraycopy(src, 0, array, 0, Math.min(20, src.length));
        } else {
            reset();
        }
    }

    public ColorMatrix(ColorMatrix src) {
        if (src != null) {
            System.arraycopy(src.array, 0, array, 0, 20);
        } else {
            reset();
        }
    }

    public final float[] getArray() {
        return array;
    }

    public void reset() {
        Arrays.fill(array, 0f);
        array[0] = array[6] = array[12] = array[18] = 1f;
    }

    public void set(ColorMatrix src) {
        if (src != null) System.arraycopy(src.array, 0, array, 0, 20);
    }

    public void set(float[] src) {
        if (src != null) System.arraycopy(src, 0, array, 0, Math.min(20, src.length));
    }

    public void setScale(float rScale, float gScale, float bScale, float aScale) {
        reset();
        array[0] = rScale;
        array[6] = gScale;
        array[12] = bScale;
        array[18] = aScale;
    }

    public void setSaturation(float sat) {
        reset();
        float invSat = 1 - sat;
        float r = 0.213f * invSat;
        float g = 0.715f * invSat;
        float b = 0.072f * invSat;
        array[0] = r + sat;
        array[1] = g;
        array[2] = b;
        array[5] = r;
        array[6] = g + sat;
        array[7] = b;
        array[10] = r;
        array[11] = g;
        array[12] = b + sat;
    }
}
