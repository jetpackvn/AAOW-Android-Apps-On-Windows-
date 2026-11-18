package android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedHashMap;
import java.util.Map;

public class RemoteViews implements LayoutInflater.Filter, Parcelable {
    private final String packageName;
    private final int layoutId;
    private final Map<Integer, CharSequence> textOverrides = new LinkedHashMap<>();
    private final Map<Integer, Integer> visibilityOverrides = new LinkedHashMap<>();

    public RemoteViews(String packageName, int layoutId) {
        this.packageName = packageName;
        this.layoutId = layoutId;
    }

    public RemoteViews(String packageName, int layoutId, int viewId) {
        this(packageName, layoutId);
    }

    public RemoteViews(RemoteViews src) {
        this.packageName = src != null ? src.packageName : "";
        this.layoutId = src != null ? src.layoutId : 0;
        if (src != null) {
            this.textOverrides.putAll(src.textOverrides);
            this.visibilityOverrides.putAll(src.visibilityOverrides);
        }
    }

    public String getPackage() { return packageName; }
    public int getLayoutId() { return layoutId; }

    public void setTextViewText(int viewId, CharSequence text) {
        textOverrides.put(viewId, text);
    }

    public void setViewVisibility(int viewId, int visibility) {
        visibilityOverrides.put(viewId, visibility);
    }

    public void setImageViewResource(int viewId, int srcId) {
    }

    public void setImageViewBitmap(int viewId, Bitmap bitmap) {
    }

    public void setImageViewIcon(int viewId, Icon icon) {
    }

    public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
    }

    public void setProgressBar(int viewId, int max, int progress, boolean indeterminate) {
    }

    public void setTextColor(int viewId, int color) {
    }

    public void setInt(int viewId, String methodName, int value) {
    }

    public void setBoolean(int viewId, String methodName, boolean value) {
    }

    public void setCharSequence(int viewId, String methodName, CharSequence value) {
        if ("setText".equals(methodName)) textOverrides.put(viewId, value);
    }

    public View apply(Context context, ViewGroup parent) {
        View root = layoutId != 0 ? LayoutInflater.from(context).inflate(layoutId, parent, false) : new FrameLayout(context);
        reapply(context, root);
        return root;
    }

    public void reapply(Context context, View v) {
        if (v == null) return;
        for (Map.Entry<Integer, CharSequence> e : textOverrides.entrySet()) {
            View target = v.findViewById(e.getKey());
            if (target instanceof TextView tv) tv.setText(e.getValue());
        }
        for (Map.Entry<Integer, Integer> e : visibilityOverrides.entrySet()) {
            View target = v.findViewById(e.getKey());
            if (target != null) target.setVisibility(e.getValue());
        }
    }

    @Override
    public boolean onLoadClass(Class clazz) {
        return true;
    }

    @Override
    public RemoteViews clone() {
        return new RemoteViews(this);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeInt(layoutId);
    }

    public static final Creator<RemoteViews> CREATOR = new Creator<>() {
        @Override
        public RemoteViews createFromParcel(Parcel in) {
            return new RemoteViews(in.readString(), in.readInt());
        }

        @Override
        public RemoteViews[] newArray(int size) { return new RemoteViews[size]; }
    };
}
