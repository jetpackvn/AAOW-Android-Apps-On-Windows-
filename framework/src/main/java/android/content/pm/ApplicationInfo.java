package android.content.pm;

import android.os.Parcel;

public class ApplicationInfo extends PackageItemInfo {
    public String sourceDir;
    public String dataDir;
    public int theme;
    public int targetSdkVersion = 35;
    public int minSdkVersion = 21;
    public int flags;

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(sourceDir);
        dest.writeString(dataDir);
        dest.writeInt(theme);
        dest.writeInt(targetSdkVersion);
        dest.writeInt(minSdkVersion);
        dest.writeInt(flags);
    }
}
