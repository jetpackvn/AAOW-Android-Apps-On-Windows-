package android.util;

import java.util.TreeMap;

public class SparseBooleanArray implements Cloneable {
    private final TreeMap<Integer, Boolean> map = new TreeMap<>();

    public SparseBooleanArray() {
    }

    public SparseBooleanArray(int initialCapacity) {
    }

    @Override
    public SparseBooleanArray clone() {
        SparseBooleanArray copy = new SparseBooleanArray();
        copy.map.putAll(map);
        return copy;
    }

    public boolean get(int key) {
        return get(key, false);
    }

    public boolean get(int key, boolean valueIfKeyNotFound) {
        Boolean v = map.get(key);
        return v != null ? v : valueIfKeyNotFound;
    }

    public void delete(int key) {
        map.remove(key);
    }

    public void put(int key, boolean value) {
        map.put(key, value);
    }

    public int size() {
        return map.size();
    }

    public int keyAt(int index) {
        int i = 0;
        for (Integer k : map.keySet()) {
            if (i++ == index) return k;
        }
        throw new IndexOutOfBoundsException("index=" + index);
    }

    public boolean valueAt(int index) {
        int i = 0;
        for (Boolean v : map.values()) {
            if (i++ == index) return v;
        }
        throw new IndexOutOfBoundsException("index=" + index);
    }

    public void setValueAt(int index, boolean value) {
        put(keyAt(index), value);
    }

    public int indexOfKey(int key) {
        int i = 0;
        for (Integer k : map.keySet()) {
            if (k == key) return i;
            i++;
        }
        return -1;
    }

    public int indexOfValue(boolean value) {
        int i = 0;
        for (Boolean v : map.values()) {
            if (v == value) return i;
            i++;
        }
        return -1;
    }

    public void clear() {
        map.clear();
    }

    public void append(int key, boolean value) {
        put(key, value);
    }
}
