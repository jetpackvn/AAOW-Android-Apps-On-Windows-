package android.content.pm;

import android.os.Parcel;

public class ApplicationInfo extends PackageItemInfo {
    public static final int FLAG_SYSTEM = 1 << 0;
    public static final int FLAG_DEBUGGABLE = 1 << 1;

    public String sourceDir;
    public String publicSourceDir;
    public String dataDir;
    public String nativeLibraryDir;
    public String processName;
    public int uid = 10000;
    public int theme;
    public int targetSdkVersion = 35;
    public int minSdkVersion = 21;
    public int flags;
    public boolean enabled = true;

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(sourceDir);
        dest.writeString(publicSourceDir);
        dest.writeString(dataDir);
        dest.writeString(nativeLibraryDir);
        dest.writeString(processName);
        dest.writeInt(uid);
        dest.writeInt(theme);
        dest.writeInt(targetSdkVersion);
        dest.writeInt(minSdkVersion);
        dest.writeInt(flags);
    }
}
