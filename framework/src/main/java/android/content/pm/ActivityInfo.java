package android.content.pm;

import android.os.Parcel;

public class ActivityInfo extends ComponentInfo {
    public int theme;
    public int configChanges;
    public int uiOptions;
    public int screenOrientation = -1;
    public int softInputMode;

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeInt(theme);
    }
}
