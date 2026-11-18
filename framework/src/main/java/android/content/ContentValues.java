package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ContentValues implements Parcelable {
    private final Map<String, Object> values;

    public ContentValues() {
        this.values = new LinkedHashMap<>();
    }

    public ContentValues(int size) {
        this.values = new LinkedHashMap<>(Math.max(4, size));
    }

    public ContentValues(ContentValues from) {
        this.values = from != null ? new LinkedHashMap<>(from.values) : new LinkedHashMap<>();
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

    public void put(String key, Byte value) {
        values.put(key, value);
    }

    public void put(String key, Short value) {
        values.put(key, value);
    }

    public void put(String key, Integer value) {
        values.put(key, value);
    }

    public void put(String key, Long value) {
        values.put(key, value);
    }

    public void put(String key, Float value) {
        values.put(key, value);
    }

    public void put(String key, Double value) {
        values.put(key, value);
    }

    public void put(String key, Boolean value) {
        values.put(key, value);
    }

    public void put(String key, byte[] value) {
        values.put(key, value);
    }

    public void putNull(String key) {
        values.put(key, null);
    }

    public void putAll(ContentValues other) {
        if (other != null) values.putAll(other.values);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void remove(String key) {
        values.remove(key);
    }

    public void clear() {
        values.clear();
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getAsString(String key) {
        Object v = values.get(key);
        return v != null ? v.toString() : null;
    }

    public Long getAsLong(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public Integer getAsInteger(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public Double getAsDouble(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public Float getAsFloat(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) return n.floatValue();
        if (v instanceof String s) {
            try {
                return Float.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public Boolean getAsBoolean(String key) {
        Object v = values.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    public byte[] getAsByteArray(String key) {
        Object v = values.get(key);
        return v instanceof byte[] arr ? arr : null;
    }

    public Set<Map.Entry<String, Object>> valueSet() {
        return values.entrySet();
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(values.size());
        for (var e : values.entrySet()) {
            parcel.writeString(e.getKey());
            parcel.writeString(e.getValue() != null ? e.getValue().toString() : null);
        }
    }

    public static final Creator<ContentValues> CREATOR = new Creator<>() {
        @Override
        public ContentValues createFromParcel(Parcel in) {
            int n = in.readInt();
            ContentValues cv = new ContentValues(Math.max(4, n));
            for (int i = 0; i < n; i++) {
                String k = in.readString();
                String v = in.readString();
                cv.put(k, v);
            }
            return cv;
        }

        @Override
        public ContentValues[] newArray(int size) {
            return new ContentValues[size];
        }
    };
}
