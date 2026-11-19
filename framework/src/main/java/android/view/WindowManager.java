package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public interface WindowManager extends ViewManager {
    Display getDefaultDisplay();

    default WindowMetrics getCurrentWindowMetrics() {
        Display d = getDefaultDisplay();
        int w = d != null && d.getWidth() > 0 ? d.getWidth() : 420;
        int h = d != null && d.getHeight() > 0 ? d.getHeight() : 820;
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        if (d != null) d.getMetrics(dm);
        return new WindowMetrics(new android.graphics.Rect(0, 0, w, h),
                new WindowInsets(android.graphics.Insets.NONE, false),
                dm.density > 0f ? dm.density : 1.5f);
    }

    default WindowMetrics getMaximumWindowMetrics() {
        return getCurrentWindowMetrics();
    }

    class LayoutParams extends ViewGroup.LayoutParams implements Parcelable {
        public static final int FLAG_KEEP_SCREEN_ON = 0x00000080;
        public static final int FLAG_FULLSCREEN = 0x00000400;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x00000010;
        public static final int SOFT_INPUT_STATE_HIDDEN = 0x00000002;
        public static final int TYPE_APPLICATION = 2;

        public int x;
        public int y;
        public int type = TYPE_APPLICATION;
        public int flags;
        public int softInputMode;
        public int format = -1;
        public int gravity;
        public float alpha = 1.0f;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int _type, int _flags, int _format) {
            super(w, h);
            this.type = _type;
            this.flags = _flags;
            this.format = _format;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int parcelableFlags) {
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(flags);
        }
    }
}
