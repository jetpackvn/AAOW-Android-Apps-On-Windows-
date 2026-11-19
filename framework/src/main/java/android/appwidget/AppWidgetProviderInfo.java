package android.appwidget;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

/** Thông tin metadata của một AppWidget trên màn hình chính. */
public class AppWidgetProviderInfo implements Parcelable {
    public ComponentName provider;
    public int minWidth;
    public int minHeight;
    public int minResizeWidth;
    public int minResizeHeight;
    public int updatePeriodMillis;
    public int initialLayout;
    public ComponentName configure;
    public String label;
    public int icon;
    public int autoAdvanceViewId;
    public int previewImage;
    public int resizeMode;
    public int widgetCategory;

    public AppWidgetProviderInfo() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(provider, flags);
        out.writeInt(minWidth);
        out.writeInt(minHeight);
        out.writeInt(updatePeriodMillis);
        out.writeInt(initialLayout);
        out.writeString(label);
    }

    public static final Creator<AppWidgetProviderInfo> CREATOR = new Creator<>() {
        @Override
        public AppWidgetProviderInfo createFromParcel(Parcel in) {
            AppWidgetProviderInfo info = new AppWidgetProviderInfo();
            info.provider = in.readParcelable(null);
            info.minWidth = in.readInt();
            info.minHeight = in.readInt();
            info.updatePeriodMillis = in.readInt();
            info.initialLayout = in.readInt();
            info.label = in.readString();
            return info;
        }

        @Override
        public AppWidgetProviderInfo[] newArray(int size) {
            return new AppWidgetProviderInfo[size];
        }
    };
}
