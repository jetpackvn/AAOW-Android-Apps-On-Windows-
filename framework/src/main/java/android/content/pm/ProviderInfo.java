package android.content.pm;

import android.os.Parcel;
import android.os.PatternMatcher;

/** Thông tin ContentProvider lấy từ AndroidManifest.xml. */
public final class ProviderInfo extends ComponentInfo {
    public static final int FLAG_SINGLE_USER = 0x40000000;

    public String authority;
    public String readPermission;
    public String writePermission;
    public boolean grantUriPermissions;
    public boolean forceUriPermissions;
    public PatternMatcher[] uriPermissionPatterns;
    public PathPermission[] pathPermissions;
    public boolean multiprocess;
    public int initOrder;
    public int flags;
    public boolean isSyncable;

    public ProviderInfo() {
    }

    public ProviderInfo(ProviderInfo orig) {
        super(orig);
        if (orig != null) {
            this.authority = orig.authority;
            this.readPermission = orig.readPermission;
            this.writePermission = orig.writePermission;
            this.grantUriPermissions = orig.grantUriPermissions;
            this.forceUriPermissions = orig.forceUriPermissions;
            this.uriPermissionPatterns = orig.uriPermissionPatterns;
            this.pathPermissions = orig.pathPermissions;
            this.multiprocess = orig.multiprocess;
            this.initOrder = orig.initOrder;
            this.flags = orig.flags;
            this.isSyncable = orig.isSyncable;
        }
    }

    @Override
    public void writeToParcel(Parcel out, int parcelableFlags) {
        super.writeToParcel(out, parcelableFlags);
        out.writeString(authority);
        out.writeString(readPermission);
        out.writeString(writePermission);
        out.writeBoolean(grantUriPermissions);
        out.writeBoolean(forceUriPermissions);
        out.writeBoolean(multiprocess);
        out.writeInt(initOrder);
        out.writeInt(flags);
        out.writeBoolean(isSyncable);
    }

    public static final Creator<ProviderInfo> CREATOR = new Creator<>() {
        @Override
        public ProviderInfo createFromParcel(Parcel in) {
            ProviderInfo pi = new ProviderInfo();
            pi.name = in.readString();
            pi.packageName = in.readString();
            return pi;
        }

        @Override
        public ProviderInfo[] newArray(int size) {
            return new ProviderInfo[size];
        }
    };

    @Override
    public String toString() {
        return "ContentProviderInfo{name=" + authority + " className=" + name + "}";
    }
}
