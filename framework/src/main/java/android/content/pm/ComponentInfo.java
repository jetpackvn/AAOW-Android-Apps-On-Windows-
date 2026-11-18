package android.content.pm;

import android.os.Parcel;

public class ComponentInfo extends PackageItemInfo {
    public ApplicationInfo applicationInfo;
    public String processName;
    public boolean enabled = true;
    public boolean exported;

    public ComponentInfo() {
    }

    public ComponentInfo(ComponentInfo orig) {
        super(orig);
        if (orig != null) {
            this.applicationInfo = orig.applicationInfo;
            this.processName = orig.processName;
            this.enabled = orig.enabled;
            this.exported = orig.exported;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeParcelable(applicationInfo, parcelableFlags);
        dest.writeString(processName);
        dest.writeBoolean(enabled);
        dest.writeBoolean(exported);
    }
}
