package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public final class Insets implements Parcelable {
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    private Insets(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static Insets of(int left, int top, int right, int bottom) {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) return NONE;
        return new Insets(left, top, right, bottom);
    }

    public static Insets of(Rect r) {
        return r == null ? NONE : of(r.left, r.top, r.right, r.bottom);
    }

    public static Insets add(Insets a, Insets b) {
        return of(a.left + b.left, a.top + b.top, a.right + b.right, a.bottom + b.bottom);
    }

    public static Insets subtract(Insets a, Insets b) {
        return of(a.left - b.left, a.top - b.top, a.right - b.right, a.bottom - b.bottom);
    }

    public static Insets max(Insets a, Insets b) {
        return of(Math.max(a.left, b.left), Math.max(a.top, b.top),
                Math.max(a.right, b.right), Math.max(a.bottom, b.bottom));
    }

    public static Insets min(Insets a, Insets b) {
        return of(Math.min(a.left, b.left), Math.min(a.top, b.top),
                Math.min(a.right, b.right), Math.min(a.bottom, b.bottom));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(left);
        out.writeInt(top);
        out.writeInt(right);
        out.writeInt(bottom);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Insets other
                && left == other.left && top == other.top
                && right == other.right && bottom == other.bottom;
    }

    @Override
    public int hashCode() {
        return ((left * 31 + top) * 31 + right) * 31 + bottom;
    }
}
