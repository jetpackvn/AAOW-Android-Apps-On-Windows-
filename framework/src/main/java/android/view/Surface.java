package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public class Surface implements Parcelable {
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;

    private boolean valid = true;

    public Surface() {
    }

    public boolean isValid() {
        return valid;
    }

    public void release() {
        valid = false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(valid ? 1 : 0);
    }

    public static final Creator<Surface> CREATOR = new Creator<>() {
        @Override
        public Surface createFromParcel(Parcel source) {
            return new Surface();
        }

        @Override
        public Surface[] newArray(int size) {
            return new Surface[size];
        }
    };
}
