package android.content.pm;

import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Printer;

/** Thông tin phân giải thành phần Activity/Service/Provider khớp với một Intent. */
public class ResolveInfo implements Parcelable {
    public ActivityInfo activityInfo;
    public ServiceInfo serviceInfo;
    public ProviderInfo providerInfo;
    public IntentFilter filter;
    public String resolvePackageName;
    public int priority;
    public int preferredOrder;
    public int match;
    public int specificIndex = -1;
    public boolean isDefault;
    public boolean isInstantAppAvailable;
    public int labelRes;
    public CharSequence nonLocalizedLabel;
    public int icon;

    public ResolveInfo() {
    }

    public ResolveInfo(ResolveInfo orig) {
        if (orig == null) return;
        this.activityInfo = orig.activityInfo;
        this.serviceInfo = orig.serviceInfo;
        this.providerInfo = orig.providerInfo;
        this.filter = orig.filter;
        this.resolvePackageName = orig.resolvePackageName;
        this.priority = orig.priority;
        this.preferredOrder = orig.preferredOrder;
        this.match = orig.match;
        this.specificIndex = orig.specificIndex;
        this.isDefault = orig.isDefault;
        this.isInstantAppAvailable = orig.isInstantAppAvailable;
        this.labelRes = orig.labelRes;
        this.nonLocalizedLabel = orig.nonLocalizedLabel;
        this.icon = orig.icon;
    }

    public CharSequence loadLabel(PackageManager pm) {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;
        if (activityInfo != null && activityInfo.nonLocalizedLabel != null) return activityInfo.nonLocalizedLabel;
        if (serviceInfo != null && serviceInfo.nonLocalizedLabel != null) return serviceInfo.nonLocalizedLabel;
        return resolvePackageName != null ? resolvePackageName : "";
    }

    public Drawable loadIcon(PackageManager pm) {
        return null;
    }

    public final int getIconResource() {
        return icon;
    }

    public boolean isCrossProfileIntentForwarderActivity() {
        return false;
    }

    public void dump(Printer pw, String prefix) {
        if (pw != null) pw.println(prefix + toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeParcelable(activityInfo, parcelableFlags);
        dest.writeParcelable(serviceInfo, parcelableFlags);
        dest.writeParcelable(providerInfo, parcelableFlags);
        dest.writeString(resolvePackageName);
        dest.writeInt(priority);
        dest.writeInt(preferredOrder);
        dest.writeInt(match);
        dest.writeInt(specificIndex);
        dest.writeInt(isDefault ? 1 : 0);
        dest.writeInt(labelRes);
        dest.writeString(nonLocalizedLabel != null ? nonLocalizedLabel.toString() : null);
        dest.writeInt(icon);
    }

    public static final Creator<ResolveInfo> CREATOR = new Creator<>() {
        @Override
        public ResolveInfo createFromParcel(Parcel source) {
            ResolveInfo r = new ResolveInfo();
            r.activityInfo = source.readParcelable(null);
            r.serviceInfo = source.readParcelable(null);
            r.providerInfo = source.readParcelable(null);
            r.resolvePackageName = source.readString();
            r.priority = source.readInt();
            r.preferredOrder = source.readInt();
            r.match = source.readInt();
            r.specificIndex = source.readInt();
            r.isDefault = source.readInt() != 0;
            r.labelRes = source.readInt();
            r.nonLocalizedLabel = source.readString();
            r.icon = source.readInt();
            return r;
        }

        @Override
        public ResolveInfo[] newArray(int size) {
            return new ResolveInfo[size];
        }
    };

    @Override
    public String toString() {
        return "ResolveInfo{activity=" + activityInfo + ", service=" + serviceInfo + ", pkg=" + resolvePackageName + "}";
    }
}
