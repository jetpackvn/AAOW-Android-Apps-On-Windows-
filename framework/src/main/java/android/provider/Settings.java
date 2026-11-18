package android.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.AndroidException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cài đặt an toàn cho android.provider.Settings (Secure / System / Global). */
public final class Settings {
    public static class SettingNotFoundException extends AndroidException {
        public SettingNotFoundException(String msg) {
            super(msg);
        }
    }

    public static class NameValueTable {
        public static final String NAME = "name";
        public static final String VALUE = "value";
    }

    private static final Map<String, String> SECURE_MAP = new ConcurrentHashMap<>(Map.of(
            "android_id", "aaow001122334455"
    ));
    private static final Map<String, String> SYSTEM_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> GLOBAL_MAP = new ConcurrentHashMap<>(Map.of(
            "device_provisioned", "1",
            "adb_enabled", "0",
            "animator_duration_scale", "1.0",
            "transition_animation_scale", "1.0",
            "window_animation_scale", "1.0"
    ));

    public static final class Secure extends NameValueTable {
        public static final String ANDROID_ID = "android_id";
        public static final Uri CONTENT_URI = Uri.parse("content://settings/secure");

        public static String getString(ContentResolver resolver, String name) {
            return name != null ? SECURE_MAP.get(name) : null;
        }

        public static boolean putString(ContentResolver resolver, String name, String value) {
            if (name != null && value != null) SECURE_MAP.put(name, value);
            return true;
        }

        public static int getInt(ContentResolver cr, String name, int def) {
            return parseIntOr(getString(cr, name), def);
        }

        public static int getInt(ContentResolver cr, String name) throws SettingNotFoundException {
            String v = getString(cr, name);
            if (v == null) throw new SettingNotFoundException(name);
            return Integer.parseInt(v);
        }

        public static long getLong(ContentResolver cr, String name, long def) {
            return parseLongOr(getString(cr, name), def);
        }

        public static float getFloat(ContentResolver cr, String name, float def) {
            return parseFloatOr(getString(cr, name), def);
        }
    }

    public static final class System extends NameValueTable {
        public static final Uri CONTENT_URI = Uri.parse("content://settings/system");

        public static String getString(ContentResolver resolver, String name) {
            return name != null ? SYSTEM_MAP.get(name) : null;
        }

        public static boolean putString(ContentResolver resolver, String name, String value) {
            if (name != null && value != null) SYSTEM_MAP.put(name, value);
            return true;
        }

        public static int getInt(ContentResolver cr, String name, int def) {
            return parseIntOr(getString(cr, name), def);
        }

        public static long getLong(ContentResolver cr, String name, long def) {
            return parseLongOr(getString(cr, name), def);
        }

        public static float getFloat(ContentResolver cr, String name, float def) {
            return parseFloatOr(getString(cr, name), def);
        }

        public static boolean canWrite(android.content.Context context) {
            return true;
        }
    }

    public static final class Global extends NameValueTable {
        public static final String DEVICE_PROVISIONED = "device_provisioned";
        public static final String ADB_ENABLED = "adb_enabled";
        public static final String ANIMATOR_DURATION_SCALE = "animator_duration_scale";
        public static final Uri CONTENT_URI = Uri.parse("content://settings/global");

        public static String getString(ContentResolver resolver, String name) {
            return name != null ? GLOBAL_MAP.get(name) : null;
        }

        public static boolean putString(ContentResolver resolver, String name, String value) {
            if (name != null && value != null) GLOBAL_MAP.put(name, value);
            return true;
        }

        public static int getInt(ContentResolver cr, String name, int def) {
            return parseIntOr(getString(cr, name), def);
        }

        public static long getLong(ContentResolver cr, String name, long def) {
            return parseLongOr(getString(cr, name), def);
        }

        public static float getFloat(ContentResolver cr, String name, float def) {
            return parseFloatOr(getString(cr, name), def);
        }
    }

    private static int parseIntOr(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLongOr(String s, long def) {
        if (s == null) return def;
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloatOr(String s, float def) {
        if (s == null) return def;
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return def;
        }
    }
}
