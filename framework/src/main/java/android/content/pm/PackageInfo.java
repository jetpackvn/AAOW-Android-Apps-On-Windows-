package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageInfo implements Parcelable {
    public String packageName;
    public String versionName;
    public int versionCode;
    public long longVersionCode;
    public ApplicationInfo applicationInfo;
    public ActivityInfo[] activities;

    public long getLongVersionCode() {
        return longVersionCode != 0 ? longVersionCode : versionCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(versionName);
        dest.writeInt(versionCode);
        dest.writeLong(longVersionCode);
        dest.writeParcelable(applicationInfo, flags);
    }
}
