package android.content.res;

import java.util.Locale;

public final class Configuration {
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
    public int orientation = ORIENTATION_PORTRAIT;
    public int uiMode = UI_MODE_NIGHT_NO;
    public int screenWidthDp = 412;
    public int screenHeightDp = 840;
    public int smallestScreenWidthDp = 412;
    public int densityDpi = 160;

    public void setToDefaults() {
        fontScale = 1.0f;
        locale = Locale.getDefault();
        orientation = ORIENTATION_PORTRAIT;
        uiMode = UI_MODE_NIGHT_NO;
        screenWidthDp = 412;
        screenHeightDp = 840;
        smallestScreenWidthDp = 412;
        densityDpi = 160;
    }

    public void setLocale(Locale loc) {
        this.locale = loc != null ? loc : Locale.getDefault();
    }
}
