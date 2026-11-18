package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;

public class ColorStateList implements Parcelable {
    private final int defaultColor;

    public ColorStateList(int[][] states, int[] colors) {
        this.defaultColor = (colors != null && colors.length > 0) ? colors[0] : 0xFF000000;
    }

    public static ColorStateList valueOf(int color) {
        return new ColorStateList(new int[][]{{}}, new int[]{color});
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public int getColorForState(int[] stateSet, int defaultColor) {
        return this.defaultColor;
    }

    public boolean isStateful() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(defaultColor);
    }
}
