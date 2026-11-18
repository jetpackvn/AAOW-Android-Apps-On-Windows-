package android.content.pm;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class PackageItemInfo implements Parcelable {
    public String name;
    public String packageName;
    public int labelRes;
    public CharSequence nonLocalizedLabel;
    public int icon;
    public Bundle metaData;

    public PackageItemInfo() {
    }

    public PackageItemInfo(PackageItemInfo orig) {
        if (orig != null) {
            this.name = orig.name;
            this.packageName = orig.packageName;
            this.labelRes = orig.labelRes;
            this.nonLocalizedLabel = orig.nonLocalizedLabel;
            this.icon = orig.icon;
            this.metaData = orig.metaData != null ? new Bundle(orig.metaData) : null;
        }
    }

    public CharSequence loadLabel(PackageManager pm) {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;
        return name != null ? name : packageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeString(name);
        dest.writeString(packageName);
        dest.writeInt(labelRes);
        dest.writeString(nonLocalizedLabel != null ? nonLocalizedLabel.toString() : null);
        dest.writeInt(icon);
        dest.writeBundle(metaData);
    }
}
