package android.os;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Kho dữ liệu key-value truyền giữa các Activity, Intent, onSaveInstanceState. */
public class BaseBundle {
    protected final Map<String, Object> map = new HashMap<>();

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void putAll(BaseBundle bundle) {
        if (bundle != null) map.putAll(bundle.map);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public void putBoolean(String key, boolean value) {
        map.put(key, value);
    }

    public void putInt(String key, int value) {
        map.put(key, value);
    }

    public void putLong(String key, long value) {
        map.put(key, value);
    }

    public void putDouble(String key, double value) {
        map.put(key, value);
    }

    public void putString(String key, String value) {
        map.put(key, value);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = map.get(key);
        return o instanceof Boolean b ? b : defaultValue;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        Object o = map.get(key);
        return o instanceof Number n ? n.intValue() : defaultValue;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        Object o = map.get(key);
        return o instanceof Number n ? n.longValue() : defaultValue;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        Object o = map.get(key);
        return o instanceof Number n ? n.doubleValue() : defaultValue;
    }

    public String getString(String key) {
        Object o = map.get(key);
        return o instanceof String s ? s : null;
    }

    public String getString(String key, String defaultValue) {
        String s = getString(key);
        return s != null ? s : defaultValue;
    }
}
