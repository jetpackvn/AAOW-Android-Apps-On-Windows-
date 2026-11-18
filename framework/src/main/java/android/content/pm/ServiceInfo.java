package android.content.pm;

import android.os.Parcel;

public class ServiceInfo extends ComponentInfo {
    public String permission;
    public int flags;

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(permission);
        dest.writeInt(flags);
    }
}
