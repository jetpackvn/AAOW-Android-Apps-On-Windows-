package android.util;

import java.util.Arrays;

public class SparseIntArray implements Cloneable {
    private int[] keys;
    private int[] values;
    private int size;

    public SparseIntArray() {
        this(10);
    }

    public SparseIntArray(int initialCapacity) {
        int cap = Math.max(0, initialCapacity);
        this.keys = new int[cap];
        this.values = new int[cap];
    }

    public void put(int key, int value) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i >= 0) {
            values[i] = value;
            return;
        }
        int insertAt = ~i;
        ensureCapacity(size + 1);
        if (insertAt < size) {
            System.arraycopy(keys, insertAt, keys, insertAt + 1, size - insertAt);
            System.arraycopy(values, insertAt, values, insertAt + 1, size - insertAt);
        }
        keys[insertAt] = key;
        values[insertAt] = value;
        size++;
    }

    public void append(int key, int value) {
        if (size != 0 && key <= keys[size - 1]) {
            put(key, value);
            return;
        }
        ensureCapacity(size + 1);
        keys[size] = key;
        values[size] = value;
        size++;
    }

    public int get(int key) {
        return get(key, 0);
    }

    public int get(int key, int valueIfKeyNotFound) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        return i >= 0 ? values[i] : valueIfKeyNotFound;
    }

    public void delete(int key) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i >= 0) removeAt(i);
    }

    public void removeAt(int index) {
        if (index < 0 || index >= size) return;
        System.arraycopy(keys, index + 1, keys, index, size - index - 1);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        size--;
    }

    public int size() {
        return size;
    }

    public int keyAt(int index) {
        return keys[index];
    }

    public int valueAt(int index) {
        return values[index];
    }

    public void setValueAt(int index, int value) {
        values[index] = value;
    }

    public int indexOfKey(int key) {
        return Arrays.binarySearch(keys, 0, size, key);
    }

    public int indexOfValue(int value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) return i;
        }
        return -1;
    }

    public void clear() {
        size = 0;
    }

    private void ensureCapacity(int minCap) {
        if (keys.length >= minCap) return;
        int newCap = Math.max(minCap, keys.length == 0 ? 10 : keys.length * 2);
        keys = Arrays.copyOf(keys, newCap);
        values = Arrays.copyOf(values, newCap);
    }

    @Override
    public SparseIntArray clone() {
        SparseIntArray copy = new SparseIntArray(size);
        System.arraycopy(keys, 0, copy.keys, 0, size);
        System.arraycopy(values, 0, copy.values, 0, size);
        copy.size = size;
        return copy;
    }

    @Override
    public String toString() {
        if (size == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(keys[i]).append('=').append(values[i]);
        }
        return sb.append('}').toString();
    }
}
