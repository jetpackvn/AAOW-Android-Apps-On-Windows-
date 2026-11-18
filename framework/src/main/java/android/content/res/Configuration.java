package android.content.res;

import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public final class Configuration implements Comparable<Configuration>, Parcelable {
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;

    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;

    public float fontScale = 1.0f;
    public int mcc;
    public int mnc;
    public Locale locale = Locale.getDefault();
    private LocaleList localeList = LocaleList.getDefault();
    public int orientation = ORIENTATION_PORTRAIT;
    public int uiMode = UI_MODE_NIGHT_NO;
    public int screenWidthDp = 412;
    public int screenHeightDp = 840;
    public int smallestScreenWidthDp = 412;
    public int densityDpi = 160;

    public Configuration() {
    }

    public Configuration(Configuration o) {
        setTo(o);
    }

    public void setTo(Configuration o) {
        if (o == null) return;
        this.fontScale = o.fontScale;
        this.mcc = o.mcc;
        this.mnc = o.mnc;
        this.locale = o.locale;
        this.localeList = o.localeList;
        this.orientation = o.orientation;
        this.uiMode = o.uiMode;
        this.screenWidthDp = o.screenWidthDp;
        this.screenHeightDp = o.screenHeightDp;
        this.smallestScreenWidthDp = o.smallestScreenWidthDp;
        this.densityDpi = o.densityDpi;
    }

    public void setToDefaults() {
        fontScale = 1.0f;
        locale = Locale.getDefault();
        localeList = LocaleList.getDefault();
        orientation = ORIENTATION_PORTRAIT;
        uiMode = UI_MODE_NIGHT_NO;
        screenWidthDp = 412;
        screenHeightDp = 840;
        smallestScreenWidthDp = 412;
        densityDpi = 160;
    }

    public void setLocale(Locale loc) {
        this.locale = loc != null ? loc : Locale.getDefault();
        this.localeList = new LocaleList(this.locale);
    }

    public LocaleList getLocales() {
        return localeList != null ? localeList : new LocaleList(locale != null ? locale : Locale.getDefault());
    }

    public void setLocales(LocaleList locales) {
        this.localeList = (locales == null || locales.isEmpty()) ? LocaleList.getEmptyLocaleList() : locales;
        Locale first = this.localeList.get(0);
        this.locale = first != null ? first : Locale.getDefault();
    }

    public int getLayoutDirection() {
        return 0; // View.LAYOUT_DIRECTION_LTR
    }

    @Override
    public int compareTo(Configuration that) {
        if (that == null) return 1;
        int n = Float.compare(this.fontScale, that.fontScale);
        if (n != 0) return n;
        n = Integer.compare(this.orientation, that.orientation);
        if (n != 0) return n;
        return Integer.compare(this.uiMode, that.uiMode);
    }

    public boolean equals(Configuration that) {
        if (that == null) return false;
        if (that == this) return true;
        return compareTo(that) == 0;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Configuration c && equals(c);
    }

    @Override
    public int hashCode() {
        return Float.hashCode(fontScale) * 31 + orientation * 17 + uiMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(fontScale);
        dest.writeInt(orientation);
        dest.writeInt(uiMode);
        dest.writeInt(screenWidthDp);
        dest.writeInt(screenHeightDp);
    }
}
