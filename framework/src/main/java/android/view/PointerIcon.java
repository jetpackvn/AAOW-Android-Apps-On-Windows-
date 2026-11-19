package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/** Biểu tượng con trỏ chuột (PointerIcon) dùng bởi TabLayout, Tooltip và ViewCompat. */
public final class PointerIcon implements Parcelable {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_ARROW = 1000;
    public static final int TYPE_CONTEXT_MENU = 1001;
    public static final int TYPE_HAND = 1002;
    public static final int TYPE_HELP = 1003;
    public static final int TYPE_WAIT = 1004;
    public static final int TYPE_CELL = 1006;
    public static final int TYPE_CROSSHAIR = 1007;
    public static final int TYPE_TEXT = 1008;
    public static final int TYPE_VERTICAL_TEXT = 1009;
    public static final int TYPE_ALIAS = 1010;
    public static final int TYPE_COPY = 1011;
    public static final int TYPE_NO_DROP = 1012;
    public static final int TYPE_ALL_SCROLL = 1013;
    public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;
    public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;
    public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;
    public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;
    public static final int TYPE_ZOOM_IN = 1018;
    public static final int TYPE_ZOOM_OUT = 1019;
    public static final int TYPE_GRAB = 1020;
    public static final int TYPE_GRABBING = 1021;
    public static final int TYPE_DEFAULT = TYPE_ARROW;

    private final int type;

    private PointerIcon(int type) {
        this.type = type;
    }

    public static PointerIcon getSystemIcon(Context context, int type) {
        return new PointerIcon(type);
    }

    public static PointerIcon create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
        return new PointerIcon(TYPE_DEFAULT);
    }

    public static PointerIcon load(Resources resources, int resourceId) {
        return new PointerIcon(TYPE_DEFAULT);
    }

    public int getType() {
        return type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type);
    }

    public static final Creator<PointerIcon> CREATOR = new Creator<>() {
        @Override
        public PointerIcon createFromParcel(Parcel in) {
            return new PointerIcon(in.readInt());
        }

        @Override
        public PointerIcon[] newArray(int size) {
            return new PointerIcon[size];
        }
    };

    @Override
    public boolean equals(Object other) {
        return other instanceof PointerIcon pi && this.type == pi.type;
    }

    @Override
    public int hashCode() {
        return type;
    }
}
