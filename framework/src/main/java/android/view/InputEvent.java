package android.view;

import android.os.Parcelable;

public abstract class InputEvent implements Parcelable {
    private int source;

    public int getDeviceId() {
        return 0;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public long getEventTime() {
        return System.currentTimeMillis();
    }

    public void recycle() {
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
