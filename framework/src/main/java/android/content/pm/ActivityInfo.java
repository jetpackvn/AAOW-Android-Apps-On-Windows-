package android.content.pm;

import android.os.Parcel;

public class ActivityInfo extends ComponentInfo {
    public String permission;
    public int theme;
    public int configChanges;
    public int uiOptions;
    public int screenOrientation = -1;
    public int softInputMode;
    public int flags;
    public int launchMode;
    public String parentActivityName;
    public String taskAffinity;
    public String targetActivity;

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeInt(theme);
        dest.writeString(parentActivityName);
    }
}
