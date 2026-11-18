package android.content.res;

import android.graphics.drawable.Drawable;

public class TypedArray implements AutoCloseable {
    private final Resources resources;
    private final int[] attrs;

    public TypedArray(Resources resources, int[] attrs) {
        this.resources = resources;
        this.attrs = attrs != null ? attrs : new int[0];
    }

    public int length() {
        return attrs.length;
    }

    public int getIndexCount() {
        return 0;
    }

    public int getIndex(int at) {
        return at;
    }

    public Resources getResources() {
        return resources;
    }

    public CharSequence getText(int index) {
        return null;
    }

    public String getString(int index) {
        return null;
    }

    public boolean getBoolean(int index, boolean defValue) {
        return defValue;
    }

    public int getInt(int index, int defValue) {
        return defValue;
    }

    public float getFloat(int index, float defValue) {
        return defValue;
    }

    public int getColor(int index, int defValue) {
        return defValue;
    }

    public ColorStateList getColorStateList(int index) {
        return null;
    }

    public int getInteger(int index, int defValue) {
        return defValue;
    }

    public float getDimension(int index, float defValue) {
        return defValue;
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        return defValue;
    }

    public int getDimensionPixelSize(int index, int defValue) {
        return defValue;
    }

    public int getResourceId(int index, int defValue) {
        return defValue;
    }

    public Drawable getDrawable(int index) {
        return null;
    }

    public boolean hasValue(int index) {
        return false;
    }

    public void recycle() {
    }

    @Override
    public void close() {
        recycle();
    }
}
