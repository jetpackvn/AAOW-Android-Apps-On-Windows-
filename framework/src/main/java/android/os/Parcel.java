package android.os;

import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt nhẹ của android.os.Parcel để các APK có thể đọc/ghi dữ liệu Parcelable trong bộ nhớ.
 */
public final class Parcel {
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
        return data.size();
    }

    public int dataPosition() {
        return pos;
    }

    public void setDataPosition(int pos) {
        this.pos = Math.max(0, Math.min(pos, data.size()));
    }

    public int dataAvail() {
        return Math.max(0, data.size() - pos);
    }

    private void writeItem(Object item) {
        if (pos == data.size()) {
            data.add(item);
        } else {
            data.set(pos, item);
        }
        pos++;
    }

    private Object readItem() {
        if (pos >= data.size()) return null;
        return data.get(pos++);
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
        writeItem(val);
    }

    public long readLong() {
        Object o = readItem();
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
        writeItem(val);
    }

    public double readDouble() {
        Object o = readItem();
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
        writeItem(val);
    }

    public int[] createIntArray() {
        Object o = readItem();
        return o instanceof int[] arr ? arr : new int[0];
    }

    public void writeByteArray(byte[] b) {
        writeItem(b);
    }

    public byte[] createByteArray() {
        Object o = readItem();
        return o instanceof byte[] arr ? arr : new byte[0];
    }

    public <T extends Parcelable> void writeTypedList(List<T> val) {
        writeItem(val != null ? new ArrayList<>(val) : null);
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c) {
        Object o = readItem();
        return o instanceof List<?> l ? new ArrayList<>((List<T>) l) : new ArrayList<>();
    }
}
