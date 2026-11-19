package android.os;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cài đặt tương thích cho API ẩn {@code android.os.SystemProperties} của AOSP
 * (được gọi trực tiếp hoặc qua Reflection bởi AndroidX, Firebase Analytics, OkHttp và các SDK thương mại).
 */
public final class SystemProperties {
    private static final Map<String, String> PROPS = new ConcurrentHashMap<>();
    private static final List<Runnable> CALLBACKS = new CopyOnWriteArrayList<>();

    static {
        PROPS.put("ro.build.version.sdk", Integer.toString(Build.VERSION.SDK_INT));
        PROPS.put("ro.build.version.release", Build.VERSION.RELEASE);
        PROPS.put("ro.build.version.codename", Build.VERSION.CODENAME);
        PROPS.put("ro.build.id", Build.ID);
        PROPS.put("ro.build.display.id", Build.DISPLAY);
        PROPS.put("ro.build.fingerprint", Build.FINGERPRINT);
        PROPS.put("ro.build.type", Build.TYPE);
        PROPS.put("ro.build.tags", Build.TAGS);
        PROPS.put("ro.product.brand", Build.BRAND);
        PROPS.put("ro.product.manufacturer", Build.MANUFACTURER);
        PROPS.put("ro.product.model", Build.MODEL);
        PROPS.put("ro.product.device", Build.DEVICE);
        PROPS.put("ro.product.name", Build.PRODUCT);
        PROPS.put("ro.product.board", Build.BOARD);
        PROPS.put("ro.hardware", Build.HARDWARE);
        PROPS.put("ro.product.cpu.abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "arm64-v8a");
        PROPS.put("ro.product.cpu.abilist", String.join(",", Build.SUPPORTED_ABIS));
        PROPS.put("ro.debuggable", "0");
        PROPS.put("ro.secure", "1");
        PROPS.put("persist.sys.timezone", java.util.TimeZone.getDefault().getID());
        PROPS.put("persist.sys.locale", java.util.Locale.getDefault().toLanguageTag());
    }

    private SystemProperties() {
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static String get(String key, String def) {
        if (key == null || key.isEmpty()) {
            return def != null ? def : "";
        }
        String val = PROPS.get(key);
        if (val != null) {
            return val;
        }
        String sys = System.getProperty("aaow.prop." + key);
        if (sys != null) {
            return sys;
        }
        return def != null ? def : "";
    }

    public static int getInt(String key, int def) {
        String val = get(key, "");
        if (val.isEmpty()) {
            return def;
        }
        try {
            return Integer.decode(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long getLong(String key, long def) {
        String val = get(key, "");
        if (val.isEmpty()) {
            return def;
        }
        try {
            return Long.decode(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean getBoolean(String key, boolean def) {
        String val = get(key, "").trim();
        if (val.isEmpty()) {
            return def;
        }
        if ("1".equals(val) || "y".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val)
                || "true".equalsIgnoreCase(val) || "on".equalsIgnoreCase(val)) {
            return true;
        }
        if ("0".equals(val) || "n".equalsIgnoreCase(val) || "no".equalsIgnoreCase(val)
                || "false".equalsIgnoreCase(val) || "off".equalsIgnoreCase(val)) {
            return false;
        }
        return def;
    }

    public static void set(String key, String val) {
        if (key == null || key.isEmpty()) {
            return;
        }
        if (val == null) {
            PROPS.remove(key);
        } else {
            PROPS.put(key, val);
        }
        for (Runnable cb : CALLBACKS) {
            try {
                cb.run();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void addChangeCallback(Runnable callback) {
        if (callback != null) {
            CALLBACKS.add(callback);
        }
    }

    public static void reportSyspropChanged() {
        for (Runnable cb : CALLBACKS) {
            try {
                cb.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
