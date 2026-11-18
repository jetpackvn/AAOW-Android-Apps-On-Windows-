package android.database;

import android.database.sqlite.SQLiteClosable;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Bộ đệm dạng bảng lưu kết quả truy vấn SQLite trong bộ nhớ theo cấu trúc CursorWindow của Android. */
public class CursorWindow extends SQLiteClosable implements Parcelable {
    private final String name;
    private int startPos;
    private int numColumns;
    private final List<Object[]> rows = new ArrayList<>();

    public CursorWindow(String name) {
        this.name = name != null && !name.isEmpty() ? name : "<unnamed>";
    }

    public CursorWindow(boolean localWindow) {
        this((String) null);
    }

    public CursorWindow(String name, long windowSizeBytes) {
        this(name);
    }

    public void clear() {
        acquireReference();
        try {
            startPos = 0;
            numColumns = 0;
            rows.clear();
        } finally {
            releaseReference();
        }
    }

    public int getStartPosition() {
        return startPos;
    }

    public void setStartPosition(int pos) {
        this.startPos = pos;
    }

    public int getNumRows() {
        return rows.size();
    }

    public boolean setNumColumns(int columnNum) {
        if (columnNum < 0) return false;
        if (!rows.isEmpty() && columnNum != numColumns) {
            return false;
        }
        this.numColumns = columnNum;
        return true;
    }

    public boolean allocRow() {
        rows.add(new Object[Math.max(0, numColumns)]);
        return true;
    }

    public void freeLastRow() {
        if (!rows.isEmpty()) {
            rows.remove(rows.size() - 1);
        }
    }

    private Object getCell(int row, int column) {
        int localRow = row - startPos;
        if (localRow < 0 || localRow >= rows.size()) {
            throw new IllegalStateException("CursorWindow row out of bounds: row=" + row + ", start=" + startPos + ", count=" + rows.size());
        }
        Object[] r = rows.get(localRow);
        if (column < 0 || column >= numColumns) {
            throw new IllegalStateException("CursorWindow column out of bounds: col=" + column + ", numColumns=" + numColumns);
        }
        return r[column];
    }

    private boolean putCell(Object value, int row, int column) {
        int localRow = row - startPos;
        if (localRow < 0 || localRow >= rows.size()) return false;
        if (column < 0 || column >= numColumns) return false;
        rows.get(localRow)[column] = value;
        return true;
    }

    public boolean putBlob(byte[] value, int row, int column) {
        return putCell(value, row, column);
    }

    public boolean putString(String value, int row, int column) {
        return putCell(value, row, column);
    }

    public boolean putLong(long value, int row, int column) {
        return putCell(value, row, column);
    }

    public boolean putDouble(double value, int row, int column) {
        return putCell(value, row, column);
    }

    public boolean putNull(int row, int column) {
        return putCell(null, row, column);
    }

    public int getType(int row, int column) {
        Object v = getCell(row, column);
        if (v == null) return Cursor.FIELD_TYPE_NULL;
        if (v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte) {
            return Cursor.FIELD_TYPE_INTEGER;
        }
        if (v instanceof Double || v instanceof Float) {
            return Cursor.FIELD_TYPE_FLOAT;
        }
        if (v instanceof byte[]) {
            return Cursor.FIELD_TYPE_BLOB;
        }
        return Cursor.FIELD_TYPE_STRING;
    }

    public boolean isNull(int row, int column) {
        return getType(row, column) == Cursor.FIELD_TYPE_NULL;
    }

    public boolean isBlob(int row, int column) {
        int t = getType(row, column);
        return t == Cursor.FIELD_TYPE_BLOB || t == Cursor.FIELD_TYPE_NULL;
    }

    public boolean isLong(int row, int column) {
        return getType(row, column) == Cursor.FIELD_TYPE_INTEGER;
    }

    public boolean isFloat(int row, int column) {
        return getType(row, column) == Cursor.FIELD_TYPE_FLOAT;
    }

    public boolean isString(int row, int column) {
        return getType(row, column) == Cursor.FIELD_TYPE_STRING;
    }

    public byte[] getBlob(int row, int column) {
        Object v = getCell(row, column);
        if (v == null) return null;
        if (v instanceof byte[] b) return b;
        if (v instanceof String s) return s.getBytes(StandardCharsets.UTF_8);
        throw new SQLiteException("Unable to convert cell to BLOB");
    }

    public String getString(int row, int column) {
        Object v = getCell(row, column);
        if (v == null) return null;
        if (v instanceof byte[]) {
            throw new SQLiteException("Unable to convert BLOB to string");
        }
        return v.toString();
    }

    public void copyStringToBuffer(int row, int column, CharArrayBuffer buffer) {
        if (buffer == null) throw new IllegalArgumentException("CharArrayBuffer must not be null");
        String s = getString(row, column);
        if (s == null) {
            buffer.sizeCopied = 0;
            return;
        }
        char[] chars = s.toCharArray();
        if (buffer.data == null || buffer.data.length < chars.length) {
            buffer.data = chars;
        } else {
            System.arraycopy(chars, 0, buffer.data, 0, chars.length);
        }
        buffer.sizeCopied = chars.length;
    }

    public long getLong(int row, int column) {
        Object v = getCell(row, column);
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                try {
                    return (long) Double.parseDouble(s.trim());
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        throw new SQLiteException("Unable to convert BLOB to long");
    }

    public int getInt(int row, int column) {
        return (int) getLong(row, column);
    }

    public short getShort(int row, int column) {
        return (short) getLong(row, column);
    }

    public double getDouble(int row, int column) {
        Object v = getCell(row, column);
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        throw new SQLiteException("Unable to convert BLOB to double");
    }

    public float getFloat(int row, int column) {
        return (float) getDouble(row, column);
    }

    @Override
    protected void onAllReferencesReleased() {
        rows.clear();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(startPos);
        dest.writeInt(numColumns);
        dest.writeInt(rows.size());
        for (Object[] r : rows) {
            for (int c = 0; c < numColumns; c++) {
                dest.writeValue(r[c]);
            }
        }
    }

    public static CursorWindow newFromParcel(Parcel p) {
        return CREATOR.createFromParcel(p);
    }

    public static final Creator<CursorWindow> CREATOR = new Creator<>() {
        @Override
        public CursorWindow createFromParcel(Parcel in) {
            String name = in.readString();
            int start = in.readInt();
            int cols = in.readInt();
            int count = in.readInt();
            CursorWindow w = new CursorWindow(name);
            w.setStartPosition(start);
            w.setNumColumns(cols);
            for (int i = 0; i < count; i++) {
                w.allocRow();
                for (int c = 0; c < cols; c++) {
                    Object v = in.readValue(null);
                    w.putCell(v, start + i, c);
                }
            }
            return w;
        }

        @Override
        public CursorWindow[] newArray(int size) {
            return new CursorWindow[size];
        }
    };

    @Override
    public String toString() {
        return name + " {rows=" + rows.size() + ", cols=" + numColumns + "}";
    }
}
