package android.database;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/** Cursor dạng bảng trong bộ nhớ thường dùng cho ContentProvider và FileProvider. */
public class MatrixCursor extends AbstractCursor {
    private final String[] columnNames;
    private Object[] data;
    private int rowCount = 0;
    private final int columnCount;

    public MatrixCursor(String[] columnNames, int initialCapacity) {
        this.columnNames = columnNames != null ? columnNames : new String[0];
        this.columnCount = this.columnNames.length;
        int cap = Math.max(1, initialCapacity);
        this.data = new Object[cap * Math.max(1, this.columnCount)];
    }

    public MatrixCursor(String[] columnNames) {
        this(columnNames, 16);
    }

    private Object get(int column) {
        if (column < 0 || column >= columnCount) {
            throw new IndexOutOfBoundsException("Requested column: " + column + ", # of columns: " + columnCount);
        }
        if (mPos < 0 || mPos >= rowCount) {
            throw new IndexOutOfBoundsException("Cursor out of bounds: pos=" + mPos + ", count=" + rowCount);
        }
        return data[mPos * columnCount + column];
    }

    public RowBuilder newRow() {
        final int row = rowCount++;
        final int endIndex = rowCount * columnCount;
        ensureCapacity(endIndex);
        return new RowBuilder(row);
    }

    public void addRow(Object[] columnValues) {
        if (columnValues == null || columnValues.length != columnCount) {
            throw new IllegalArgumentException("columnNames.length = " + columnCount
                    + ", columnValues.length = " + (columnValues != null ? columnValues.length : 0));
        }
        int start = rowCount++ * columnCount;
        ensureCapacity(start + columnCount);
        System.arraycopy(columnValues, 0, data, start, columnCount);
    }

    public void addRow(Iterable<?> columnValues) {
        int start = rowCount * columnCount;
        int end = start + columnCount;
        ensureCapacity(end);

        if (columnValues instanceof ArrayList<?> list) {
            addRow(list, start);
            return;
        }

        int current = start;
        Object[] localData = data;
        for (Object columnValue : columnValues) {
            if (current == end) {
                throw new IllegalArgumentException("columnValues.size() > columnNames.length");
            }
            localData[current++] = columnValue;
        }
        if (current != end) {
            throw new IllegalArgumentException("columnValues.size() < columnNames.length");
        }
        rowCount++;
    }

    private void addRow(ArrayList<?> columnValues, int start) {
        int size = columnValues.size();
        if (size != columnCount) {
            throw new IllegalArgumentException("columnNames.length = " + columnCount
                    + ", columnValues.size() = " + size);
        }
        rowCount++;
        Object[] localData = data;
        for (int i = 0; i < size; i++) {
            localData[start + i] = columnValues.get(i);
        }
    }

    private void ensureCapacity(int size) {
        if (size > data.length) {
            int newSize = Math.max(data.length * 2, size);
            data = Arrays.copyOf(data, newSize);
        }
    }

    public class RowBuilder {
        private final int row;
        private int index;

        RowBuilder(int row) {
            this.row = row;
            this.index = row * columnCount;
        }

        public RowBuilder add(Object columnValue) {
            if (index >= (row + 1) * columnCount) {
                throw new IndexOutOfBoundsException("No more columns left.");
            }
            data[index++] = columnValue;
            return this;
        }

        public RowBuilder add(String columnName, Object value) {
            for (int i = 0; i < columnNames.length; i++) {
                if (columnName != null && columnName.equals(columnNames[i])) {
                    data[(row * columnCount) + i] = value;
                }
            }
            return this;
        }
    }

    @Override
    public int getCount() {
        return rowCount;
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public String getString(int column) {
        Object value = get(column);
        return value != null ? value.toString() : null;
    }

    @Override
    public short getShort(int column) {
        Object value = get(column);
        if (value == null) return 0;
        if (value instanceof Number n) return n.shortValue();
        return Short.parseShort(value.toString());
    }

    @Override
    public int getInt(int column) {
        Object value = get(column);
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(int column) {
        Object value = get(column);
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    @Override
    public float getFloat(int column) {
        Object value = get(column);
        if (value == null) return 0f;
        if (value instanceof Number n) return n.floatValue();
        return Float.parseFloat(value.toString());
    }

    @Override
    public double getDouble(int column) {
        Object value = get(column);
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    @Override
    public byte[] getBlob(int column) {
        Object value = get(column);
        if (value == null) return null;
        if (value instanceof byte[] b) return b;
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int getType(int column) {
        Object value = get(column);
        if (value == null) return FIELD_TYPE_NULL;
        if (value instanceof byte[]) return FIELD_TYPE_BLOB;
        if (value instanceof Float || value instanceof Double) return FIELD_TYPE_FLOAT;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return FIELD_TYPE_INTEGER;
        }
        return FIELD_TYPE_STRING;
    }

    @Override
    public boolean isNull(int column) {
        return get(column) == null;
    }
}
