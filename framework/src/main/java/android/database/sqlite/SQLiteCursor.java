package android.database.sqlite;

import android.database.AbstractWindowedCursor;
import android.database.CursorWindow;

import java.util.HashMap;
import java.util.Map;

/** Con trỏ đọc kết quả truy vấn SQLite thực tế từ SQLiteQuery và CursorWindow. */
public class SQLiteCursor extends AbstractWindowedCursor {
    private static final int NO_COUNT = -1;

    private final SQLiteQuery query;
    private final SQLiteCursorDriver driver;
    private final String editTable;
    private String[] columns;
    private int count = NO_COUNT;
    private Map<String, Integer> columnNameMap;

    @Deprecated
    public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        this(driver, editTable, query);
    }

    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query object cannot be null");
        }
        this.driver = driver;
        this.editTable = editTable;
        this.query = query;
    }

    public SQLiteDatabase getDatabase() {
        return query.getDatabase();
    }

    private void ensureLoaded() {
        if (count != NO_COUNT && columns != null) {
            return;
        }
        CursorWindow window = mWindow;
        if (window == null) {
            window = new CursorWindow(editTable != null ? editTable : "SQLiteCursor");
            mWindow = window;
        }
        this.columns = query.fillWindow(window, 0, 0, true);
        if (this.columns == null) {
            this.columns = new String[0];
        }
        this.count = window.getNumRows();
        this.columnNameMap = null;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        ensureLoaded();
        return mWindow != null && newPosition >= mWindow.getStartPosition()
                && newPosition < mWindow.getStartPosition() + mWindow.getNumRows();
    }

    @Override
    public int getCount() {
        ensureLoaded();
        return count;
    }

    @Override
    public int getColumnIndex(String columnName) {
        ensureLoaded();
        if (columnName == null) return -1;
        if (columnNameMap == null) {
            Map<String, Integer> map = new HashMap<>(columns.length * 2);
            for (int i = 0; i < columns.length; i++) {
                map.putIfAbsent(columns[i].toLowerCase(), i);
            }
            columnNameMap = map;
        }
        Integer idx = columnNameMap.get(columnName.toLowerCase());
        if (idx != null) return idx;
        int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) {
            String suffix = columnName.substring(periodIndex + 1).toLowerCase();
            idx = columnNameMap.get(suffix);
            if (idx != null) return idx;
        }
        return -1;
    }

    @Override
    public String[] getColumnNames() {
        ensureLoaded();
        return columns;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (driver != null) {
            driver.cursorDeactivated();
        }
    }

    @Override
    public void close() {
        super.close();
        synchronized (this) {
            query.close();
            if (driver != null) {
                driver.cursorClosed();
            }
        }
    }

    @Override
    public boolean requery() {
        if (isClosed()) {
            return false;
        }
        synchronized (this) {
            if (!query.getDatabase().isOpen()) {
                return false;
            }
            count = NO_COUNT;
            columns = null;
            columnNameMap = null;
            if (mWindow != null) {
                mWindow.clear();
            }
            mPos = -1;
            if (driver != null) {
                driver.cursorRequeried(this);
            }
        }
        try {
            return super.requery();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void setWindow(CursorWindow window) {
        super.setWindow(window);
        count = NO_COUNT;
    }

    public void setSelectionArguments(String[] selectionArgs) {
        if (driver != null) {
            driver.setBindArguments(selectionArgs);
        }
    }

    public void setFillWindowForwardOnly(boolean fillWindowForwardOnly) {
    }
}
