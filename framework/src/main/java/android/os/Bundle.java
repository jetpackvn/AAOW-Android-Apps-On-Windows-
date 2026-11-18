package android.os;

import java.io.Serializable;
import java.util.ArrayList;

public final class Bundle extends BaseBundle implements Cloneable, Parcelable {
    public static final Bundle EMPTY = new Bundle();

    public Bundle() {
    }

    public Bundle(int capacity) {
    }

    public Bundle(ClassLoader loader) {
    }

    public Bundle(Bundle b) {
        if (b != null) map.putAll(b.map);
    }

    public void setClassLoader(ClassLoader loader) {
    }

    public void putAll(Bundle bundle) {
        if (bundle != null) map.putAll(bundle.map);
    }

    public void putByte(String key, byte value) {
        map.put(key, value);
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public Byte getByte(String key, byte defaultValue) {
        Object o = map.get(key);
        return o instanceof Number n ? n.byteValue() : defaultValue;
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

    public void putParcelable(String key, Parcelable value) {
        map.put(key, value);
    }

    public void putParcelableArray(String key, Parcelable[] value) {
        map.put(key, value);
    }

    public Parcelable[] getParcelableArray(String key) {
        Object o = map.get(key);
        return o instanceof Parcelable[] arr ? arr : null;
    }

    public void putCharSequenceArray(String key, CharSequence[] value) {
        map.put(key, value);
    }

    public CharSequence[] getCharSequenceArray(String key) {
        Object o = map.get(key);
        return o instanceof CharSequence[] arr ? arr : null;
    }

    public void putFloatArray(String key, float[] value) {
        map.put(key, value);
    }

    public float[] getFloatArray(String key) {
        Object o = map.get(key);
        return o instanceof float[] arr ? arr : null;
    }

    public void putByteArray(String key, byte[] value) {
        map.put(key, value);
    }

    public byte[] getByteArray(String key) {
        Object o = map.get(key);
        return o instanceof byte[] arr ? arr : null;
    }

    public void putParcelableArrayList(String key, ArrayList<? extends Parcelable> value) {
        map.put(key, value);
    }

    public void putStringArrayList(String key, ArrayList<String> value) {
        map.put(key, value);
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
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

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T getParcelable(String key) {
        Object o = map.get(key);
        return o instanceof Parcelable ? (T) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList<?> list ? (ArrayList<T>) list : null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getStringArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList<?> list ? (ArrayList<String>) list : null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Integer> getIntegerArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList<?> list ? (ArrayList<Integer>) list : null;
    }

    public int[] getIntArray(String key) {
        Object o = map.get(key);
        return o instanceof int[] arr ? arr : null;
    }

    public String[] getStringArray(String key) {
        Object o = map.get(key);
        return o instanceof String[] arr ? arr : null;
    }

    public Serializable getSerializable(String key) {
        Object o = map.get(key);
        return o instanceof Serializable s ? s : null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeBundle(this);
    }

    public static final Creator<Bundle> CREATOR = new Creator<>() {
        @Override
        public Bundle createFromParcel(Parcel in) {
            Bundle b = in.readBundle();
            return b != null ? b : new Bundle();
        }

        @Override
        public Bundle[] newArray(int size) {
            return new Bundle[size];
        }
    };

    @Override
    public Object clone() {
        return new Bundle(this);
    }

    @Override
    public String toString() {
        return "Bundle[" + map + "]";
    }
}
