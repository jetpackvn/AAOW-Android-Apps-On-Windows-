package android.preference;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    public static String getDefaultSharedPreferencesName(Context context) {
        return (context != null ? context.getPackageName() : "default") + "_preferences";
    }

    public static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), getDefaultSharedPreferencesMode());
    }

    public static void setDefaultValues(Context context, int resId, boolean readAgain) {
    }

    public static void setDefaultValues(Context context, String sharedPreferencesName, int sharedPreferencesMode,
                                        int resId, boolean readAgain) {
    }
}
