package dev.aaow.rt;

import android.content.SharedPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lưu trữ SharedPreferences của app Android xuống thư mục dữ liệu trên đĩa Windows.
 */
public final class DiskSharedPreferences implements SharedPreferences {
    private final Path file;
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final List<OnSharedPreferenceChangeListener> listeners = new CopyOnWriteArrayList<>();

    public DiskSharedPreferences(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            for (String k : props.stringPropertyNames()) {
                String raw = props.getProperty(k);
                if (raw.startsWith("I:")) values.put(k, Integer.parseInt(raw.substring(2)));
                else if (raw.startsWith("L:")) values.put(k, Long.parseLong(raw.substring(2)));
                else if (raw.startsWith("F:")) values.put(k, Float.parseFloat(raw.substring(2)));
                else if (raw.startsWith("B:")) values.put(k, Boolean.parseBoolean(raw.substring(2)));
                else if (raw.startsWith("S:")) values.put(k, raw.substring(2));
                else values.put(k, raw);
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            for (Map.Entry<String, Object> e : values.entrySet()) {
                Object v = e.getValue();
                String encoded;
                if (v instanceof Integer) encoded = "I:" + v;
                else if (v instanceof Long) encoded = "L:" + v;
                else if (v instanceof Float) encoded = "F:" + v;
                else if (v instanceof Boolean) encoded = "B:" + v;
                else encoded = "S:" + v;
                props.setProperty(e.getKey(), encoded);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "AAOW SharedPreferences");
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(values);
    }

    @Override
    public String getString(String key, String defValue) {
        Object v = values.get(key);
        return v != null ? v.toString() : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object v = values.get(key);
        return v instanceof Set<?> s ? (Set<String>) s : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object v = values.get(key);
        return v instanceof Number n ? n.intValue() : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object v = values.get(key);
        return v instanceof Number n ? n.longValue() : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object v = values.get(key);
        return v instanceof Number n ? n.floatValue() : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object v = values.get(key);
        return v instanceof Boolean b ? b : defValue;
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new Editor() {
            private final Map<String, Object> staged = new HashMap<>();
            private boolean clearAll;

            @Override
            public Editor putString(String key, String value) {
                staged.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> set) {
                staged.put(key, set);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                staged.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                staged.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                staged.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                staged.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                staged.put(key, null);
                return this;
            }

            @Override
            public Editor clear() {
                clearAll = true;
                return this;
            }

            @Override
            public boolean commit() {
                if (clearAll) values.clear();
                for (Map.Entry<String, Object> e : staged.entrySet()) {
                    if (e.getValue() == null) values.remove(e.getKey());
                    else values.put(e.getKey(), e.getValue());
                }
                save();
                for (String k : staged.keySet()) {
                    for (OnSharedPreferenceChangeListener l : listeners) {
                        l.onSharedPreferenceChanged(DiskSharedPreferences.this, k);
                    }
                }
                return true;
            }

            @Override
            public void apply() {
                commit();
            }
        };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (listener != null) listeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }
}
