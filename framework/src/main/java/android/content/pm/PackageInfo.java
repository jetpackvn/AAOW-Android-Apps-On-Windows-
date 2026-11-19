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
    public ActivityInfo[] receivers;
    public ServiceInfo[] services;
    public ProviderInfo[] providers;
    public String[] requestedPermissions;
    public Signature[] signatures;
    public SigningInfo signingInfo;
    public long firstInstallTime;
    public long lastUpdateTime;

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
        dest.writeParcelable(signingInfo, flags);
        dest.writeLong(firstInstallTime);
        dest.writeLong(lastUpdateTime);
    }

    public static final Creator<PackageInfo> CREATOR = new Creator<>() {
        @Override
        public PackageInfo createFromParcel(Parcel source) {
            PackageInfo pi = new PackageInfo();
            pi.packageName = source.readString();
            pi.versionName = source.readString();
            pi.versionCode = source.readInt();
            pi.longVersionCode = source.readLong();
            pi.applicationInfo = source.readParcelable(ApplicationInfo.class.getClassLoader());
            pi.signingInfo = source.readParcelable(SigningInfo.class.getClassLoader());
            pi.firstInstallTime = source.readLong();
            pi.lastUpdateTime = source.readLong();
            return pi;
        }

        @Override
        public PackageInfo[] newArray(int size) {
            return new PackageInfo[size];
        }
    };
}
