package android.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt nhẹ của android.os.Parcel để các APK có thể đọc/ghi dữ liệu Parcelable trong bộ nhớ.
 */
public final class Parcel {
    private static final int SLOT_BYTES = 4;
    private static final String WIDE_SLOT_TAIL = "\u0000AAOW_PARCEL_WIDE_TAIL\u0000";

    private static final java.util.concurrent.atomic.AtomicLong NEXT_NATIVE_PTR = new java.util.concurrent.atomic.AtomicLong(0x6000_0001L);
    private long mNativePtr = NEXT_NATIVE_PTR.getAndIncrement();
    private final List<Object> data = new ArrayList<>();
    private int pos;

    private Parcel() {
    }

    public static Parcel obtain() {
        return new Parcel();
    }

    public void recycle() {
        data.clear();
        pos = 0;
    }

    public int dataSize() {
        return data.size() * SLOT_BYTES;
    }

    public int dataPosition() {
        return pos * SLOT_BYTES;
    }

    public void setDataPosition(int bytePos) {
        int slot = bytePos <= 0 ? 0 : (bytePos / SLOT_BYTES);
        this.pos = Math.max(0, Math.min(slot, data.size()));
    }

    public int dataAvail() {
        return Math.max(0, (data.size() - pos) * SLOT_BYTES);
    }

    private void writeItem(Object item) {
        if (pos == data.size()) {
            data.add(item);
        } else {
            data.set(pos, item);
        }
        pos++;
    }

    private void writeWideItem(Object item) {
        writeItem(item);
        writeItem(WIDE_SLOT_TAIL);
    }

    private Object readItem() {
        if (pos >= data.size()) return null;
        return data.get(pos++);
    }

    private void consumeWideTailIfPresent() {
        if (pos < data.size() && WIDE_SLOT_TAIL.equals(data.get(pos))) {
            pos++;
        }
    }

    public void writeByte(byte val) {
        writeItem(val);
    }

    public byte readByte() {
        Object o = readItem();
        return o instanceof Number n ? n.byteValue() : 0;
    }

    public void writeInt(int val) {
        writeItem(val);
    }

    public int readInt() {
        Object o = readItem();
        return o instanceof Number n ? n.intValue() : 0;
    }

    public void writeLong(long val) {
        writeWideItem(val);
    }

    public long readLong() {
        Object o = readItem();
        consumeWideTailIfPresent();
        return o instanceof Number n ? n.longValue() : 0L;
    }

    public void writeFloat(float val) {
        writeItem(val);
    }

    public float readFloat() {
        Object o = readItem();
        return o instanceof Number n ? n.floatValue() : 0f;
    }

    public void writeDouble(double val) {
        writeWideItem(val);
    }

    public double readDouble() {
        Object o = readItem();
        consumeWideTailIfPresent();
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }

    public void writeBoolean(boolean val) {
        writeItem(val);
    }

    public boolean readBoolean() {
        Object o = readItem();
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return false;
    }

    public void writeString(String val) {
        writeItem(val);
    }

    public String readString() {
        Object o = readItem();
        return o instanceof String s ? s : null;
    }

    public void writeBundle(Bundle val) {
        writeItem(val);
    }

    public Bundle readBundle() {
        return readBundle(null);
    }

    public Bundle readBundle(ClassLoader loader) {
        Object o = readItem();
        return o instanceof Bundle b ? b : null;
    }

    public void writeParcelable(Parcelable p, int parcelableFlags) {
        writeItem(p);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T readParcelable(ClassLoader loader) {
        Object o = readItem();
        return o instanceof Parcelable ? (T) o : null;
    }

    public void writeStringList(List<String> val) {
        writeItem(val != null ? new ArrayList<>(val) : null);
    }

    public void readStringList(List<String> list) {
        List<String> src = createStringArrayList();
        if (list != null && src != null) {
            list.clear();
            list.addAll(src);
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> createStringArrayList() {
        Object o = readItem();
        return o instanceof List<?> l ? new ArrayList<>((List<String>) l) : new ArrayList<>();
    }

    public void writeIntArray(int[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public int[] createIntArray() {
        Object o = readItem();
        return o instanceof int[] arr ? arr.clone() : new int[0];
    }

    public void readIntArray(int[] val) {
        int[] src = createIntArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeStringArray(String[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public String[] createStringArray() {
        Object o = readItem();
        return o instanceof String[] arr ? arr.clone() : new String[0];
    }

    public void readStringArray(String[] val) {
        String[] src = createStringArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeByteArray(byte[] b) {
        writeItem(b != null ? b.clone() : null);
    }

    public void writeByteArray(byte[] b, int offset, int len) {
        if (b == null) {
            writeItem(null);
            return;
        }
        byte[] copy = new byte[Math.max(0, Math.min(len, b.length - offset))];
        if (copy.length > 0 && offset >= 0) {
            System.arraycopy(b, offset, copy, 0, copy.length);
        }
        writeItem(copy);
    }

    public byte[] createByteArray() {
        Object o = readItem();
        return o instanceof byte[] arr ? arr.clone() : new byte[0];
    }

    public void readByteArray(byte[] val) {
        byte[] src = createByteArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeCharArray(char[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public char[] createCharArray() {
        Object o = readItem();
        return o instanceof char[] arr ? arr.clone() : new char[0];
    }

    public void readCharArray(char[] val) {
        char[] src = createCharArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeBooleanArray(boolean[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public boolean[] createBooleanArray() {
        Object o = readItem();
        return o instanceof boolean[] arr ? arr.clone() : new boolean[0];
    }

    public void readBooleanArray(boolean[] val) {
        boolean[] src = createBooleanArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeLongArray(long[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public long[] createLongArray() {
        Object o = readItem();
        return o instanceof long[] arr ? arr.clone() : new long[0];
    }

    public void readLongArray(long[] val) {
        long[] src = createLongArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeFloatArray(float[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public float[] createFloatArray() {
        Object o = readItem();
        return o instanceof float[] arr ? arr.clone() : new float[0];
    }

    public void readFloatArray(float[] val) {
        float[] src = createFloatArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeDoubleArray(double[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public double[] createDoubleArray() {
        Object o = readItem();
        return o instanceof double[] arr ? arr.clone() : new double[0];
    }

    public void readDoubleArray(double[] val) {
        double[] src = createDoubleArray();
        if (val != null && src != null) {
            System.arraycopy(src, 0, val, 0, Math.min(src.length, val.length));
        }
    }

    public void writeBinderArray(IBinder[] val) {
        writeItem(val != null ? val.clone() : null);
    }

    public IBinder[] createBinderArray() {
        Object o = readItem();
        return o instanceof IBinder[] arr ? arr.clone() : new IBinder[0];
    }

    public void writeBinderList(List<IBinder> val) {
        writeItem(val != null ? new ArrayList<>(val) : null);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<IBinder> createBinderArrayList() {
        Object o = readItem();
        return o instanceof List<?> l ? new ArrayList<>((List<IBinder>) l) : new ArrayList<>();
    }

    public void writeSparseBooleanArray(android.util.SparseBooleanArray val) {
        writeItem(val);
    }

    public android.util.SparseBooleanArray readSparseBooleanArray() {
        Object o = readItem();
        return o instanceof android.util.SparseBooleanArray s ? s : null;
    }

    public void appendFrom(Parcel parcel, int offset, int length) {
        if (parcel == null || length <= 0) return;
        int start = Math.max(0, Math.min(offset / SLOT_BYTES, parcel.data.size()));
        int slots = (length + SLOT_BYTES - 1) / SLOT_BYTES;
        int end = Math.max(start, Math.min(start + slots, parcel.data.size()));
        for (int i = start; i < end; i++) {
            writeItem(parcel.data.get(i));
        }
    }

    public <T extends Parcelable> void writeTypedList(List<T> val) {
        writeItem(val != null ? new ArrayList<>(val) : null);
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c) {
        Object o = readItem();
        return o instanceof List<?> l ? new ArrayList<>((List<T>) l) : new ArrayList<>();
    }

    public void writeStrongBinder(IBinder val) {
        writeItem(val);
    }

    public IBinder readStrongBinder() {
        Object o = readItem();
        return o instanceof IBinder b ? b : null;
    }

    public void writeStrongInterface(IInterface val) {
        writeItem(val != null ? val.asBinder() : null);
    }

    public void writeNoException() {
        writeInt(0);
    }

    public void readException() {
        int code = readInt();
        if (code != 0) {
            String msg = readString();
            throw new RuntimeException(msg != null ? msg : "Parcel remote exception code=" + code);
        }
    }

    public void writeInterfaceToken(String interfaceName) {
        writeItem(interfaceName);
    }

    public void enforceInterface(String interfaceName) {
        readItem();
    }

    public void writeSerializable(Serializable s) {
        writeItem(s);
    }

    public Serializable readSerializable() {
        Object o = readItem();
        return o instanceof Serializable s ? s : null;
    }

    public <T> T readSerializable(ClassLoader loader, Class<T> clazz) {
        Object o = readItem();
        if (o == null || clazz == null) return null;
        return clazz.isInstance(o) ? clazz.cast(o) : null;
    }

    public <T> T readParcelable(ClassLoader loader, Class<T> clazz) {
        Object o = readItem();
        if (o == null || clazz == null) return null;
        return clazz.isInstance(o) ? clazz.cast(o) : null;
    }

    public void writeList(List<?> val) {
        writeItem(val != null ? new ArrayList<>(val) : null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void readList(List outVal, ClassLoader loader) {
        Object o = readItem();
        if (outVal != null && o instanceof List<?> l) {
            outVal.addAll(l);
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Object> readArrayList(ClassLoader loader) {
        Object o = readItem();
        return o instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
    }

    public void writeMap(Map<?, ?> val) {
        writeItem(val != null ? new HashMap<>(val) : null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void readMap(Map outVal, ClassLoader loader) {
        Object o = readItem();
        if (outVal != null && o instanceof Map<?, ?> m) {
            outVal.putAll(m);
        }
    }

    @SuppressWarnings("unchecked")
    public HashMap<Object, Object> readHashMap(ClassLoader loader) {
        Object o = readItem();
        return o instanceof Map<?, ?> m ? new HashMap<>(m) : new HashMap<>();
    }

    public void writeValue(Object v) {
        writeItem(v);
    }

    public Object readValue(ClassLoader loader) {
        return readItem();
    }

    public byte[] marshall() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeInt(data.size());
            for (Object item : data) {
                oos.writeObject(item instanceof Serializable ? item : null);
            }
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public void unmarshall(byte[] bytes, int offset, int length) {
        data.clear();
        pos = 0;
        if (bytes == null || length <= 0 || offset < 0 || offset + length > bytes.length) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes, offset, length))) {
            int count = ois.readInt();
            for (int i = 0; i < count; i++) {
                data.add(ois.readObject());
            }
        } catch (Exception ignored) {
            // Dữ liệu nhị phân thô không phải từ marshall(): lưu nguyên byte[] để tránh lỗi
            data.clear();
            byte[] slice = new byte[length];
            System.arraycopy(bytes, offset, slice, 0, length);
            data.add(slice);
        }
    }
}
