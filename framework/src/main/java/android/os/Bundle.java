package android.os;

import java.io.Serializable;

public final class Bundle extends BaseBundle implements Cloneable {
    public static final Bundle EMPTY = new Bundle();

    public Bundle() {
    }

    public Bundle(int capacity) {
    }

    public Bundle(Bundle b) {
        if (b != null) map.putAll(b.map);
    }

    public void putAll(Bundle bundle) {
        if (bundle != null) map.putAll(bundle.map);
    }

    public void putByte(String key, byte value) {
        map.put(key, value);
    }

    public void putChar(String key, char value) {
        map.put(key, value);
    }

    public void putShort(String key, short value) {
        map.put(key, value);
    }

    public void putFloat(String key, float value) {
        map.put(key, value);
    }

    public void putCharSequence(String key, CharSequence value) {
        map.put(key, value);
    }

    public void putBundle(String key, Bundle value) {
        map.put(key, value);
    }

    public void putSerializable(String key, Serializable value) {
        map.put(key, value);
    }

    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float defaultValue) {
        Object o = map.get(key);
        return o instanceof Number n ? n.floatValue() : defaultValue;
    }

    public CharSequence getCharSequence(String key) {
        Object o = map.get(key);
        return o instanceof CharSequence cs ? cs : null;
    }

    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        CharSequence cs = getCharSequence(key);
        return cs != null ? cs : defaultValue;
    }

    public Bundle getBundle(String key) {
        Object o = map.get(key);
        return o instanceof Bundle b ? b : null;
    }

    public Serializable getSerializable(String key) {
        Object o = map.get(key);
        return o instanceof Serializable s ? s : null;
    }

    @Override
    public Object clone() {
        return new Bundle(this);
    }

    @Override
    public String toString() {
        return "Bundle[" + map + "]";
    }
}
