package android.view;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Định danh cửa sổ gắn với một View hierarchy (dùng bởi androidx.transition.WindowIdApi18).
 */
public class WindowId implements Parcelable {
    private final Object token;
    private boolean focused = true;

    public WindowId(Object token) {
        this.token = token != null ? token : this;
    }

    public boolean isFocused() {
        return focused;
    }

    public void registerFocusObserver(FocusObserver observer) {
    }

    public void unregisterFocusObserver(FocusObserver observer) {
    }

    public IBinder getTarget() {
        return token instanceof IBinder b ? b : null;
    }

    @Override
    public boolean equals(Object otherObj) {
        if (otherObj instanceof WindowId other) {
            return token.equals(other.token);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return "WindowId{" + Integer.toHexString(System.identityHashCode(token)) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(focused ? 1 : 0);
    }

    public static final Parcelable.Creator<WindowId> CREATOR = new Parcelable.Creator<>() {
        @Override
        public WindowId createFromParcel(Parcel in) {
            WindowId id = new WindowId(new Object());
            id.focused = in.readInt() != 0;
            return id;
        }

        @Override
        public WindowId[] newArray(int size) {
            return new WindowId[size];
        }
    };

    public static abstract class FocusObserver {
        public FocusObserver() {
        }

        public abstract void onWindowFocusGained(WindowId token);

        public abstract void onWindowFocusLost(WindowId token);
    }
}
