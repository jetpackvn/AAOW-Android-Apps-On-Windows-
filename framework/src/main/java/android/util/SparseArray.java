package android.util;

import java.util.Arrays;

/** Cài đặt SparseArray của Android (dùng rất nhiều trong bytecode đã biên dịch). */
public class SparseArray<E> implements Cloneable {
    private static final Object DELETED = new Object();
    private int[] keys = new int[10];
    private Object[] values = new Object[10];
    private int size;

    public SparseArray() {
    }

    public SparseArray(int initialCapacity) {
        if (initialCapacity > 0) {
            keys = new int[initialCapacity];
            values = new Object[initialCapacity];
        }
    }

    public E get(int key) {
        return get(key, null);
    }

    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i < 0 || values[i] == DELETED) return valueIfKeyNotFound;
        return (E) values[i];
    }

    public void delete(int key) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i >= 0 && values[i] != DELETED) {
            System.arraycopy(keys, i + 1, keys, i, size - (i + 1));
            System.arraycopy(values, i + 1, values, i, size - (i + 1));
            size--;
        }
    }

    public void remove(int key) {
        delete(key);
    }

    public void put(int key, E value) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i >= 0) {
            values[i] = value;
        } else {
            i = ~i;
            if (size >= keys.length) {
                keys = Arrays.copyOf(keys, keys.length * 2);
                values = Arrays.copyOf(values, values.length * 2);
            }
            System.arraycopy(keys, i, keys, i + 1, size - i);
            System.arraycopy(values, i, values, i + 1, size - i);
            keys[i] = key;
            values[i] = value;
            size++;
        }
    }

    public int size() {
        return size;
    }

    public int keyAt(int index) {
        return keys[index];
    }

    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        return (E) values[index];
    }

    public void clear() {
        Arrays.fill(values, 0, size, null);
        size = 0;
    }
}
