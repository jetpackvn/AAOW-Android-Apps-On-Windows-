package android.database.sqlite;

import android.os.CancellationSignal;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/** Lớp cơ sở cho SQLiteStatement và SQLiteQuery quản lý câu SQL và tham số bind. */
public abstract class SQLiteProgram extends SQLiteClosable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final SQLiteDatabase db;
    private final String sql;
    private final CancellationSignal cancellationSignal;
    private final int uniqueId;
    private Object[] bindArgs;

    SQLiteProgram(SQLiteDatabase db, String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        this.db = db;
        this.sql = sql != null ? sql.trim() : "";
        this.cancellationSignal = cancellationSignal;
        this.uniqueId = NEXT_ID.getAndIncrement();
        if (bindArgs != null && bindArgs.length > 0) {
            this.bindArgs = Arrays.copyOf(bindArgs, bindArgs.length);
        } else {
            this.bindArgs = new Object[0];
        }
        // Không giữ reference tới db suốt vòng đời SQLiteProgram vì Room cache SharedSQLiteStatement không đóng ngay khi RoomDatabase.close()
    }

    final SQLiteDatabase getDatabase() {
        return db;
    }

    final String getSql() {
        return sql;
    }

    final CancellationSignal getCancellationSignal() {
        return cancellationSignal;
    }

    final Object[] getBindArgs() {
        return bindArgs;
    }

    public final int getUniqueId() {
        return uniqueId;
    }

    private void bind(int index, Object value) {
        if (index < 1) {
            throw new IllegalArgumentException("Cannot bind argument at index " + index + " because the index is out of range (1-based).");
        }
        if (bindArgs.length < index) {
            bindArgs = Arrays.copyOf(bindArgs, index);
        }
        bindArgs[index - 1] = value;
    }

    public void bindNull(int index) {
        bind(index, null);
    }

    public void bindLong(int index, long value) {
        bind(index, value);
    }

    public void bindDouble(int index, double value) {
        bind(index, value);
    }

    public void bindString(int index, String value) {
        if (value == null) {
            throw new IllegalArgumentException("the bind value at index " + index + " is null");
        }
        bind(index, value);
    }

    public void bindBlob(int index, byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("the bind value at index " + index + " is null");
        }
        bind(index, value);
    }

    public void clearBindings() {
        if (bindArgs != null) {
            Arrays.fill(bindArgs, null);
            bindArgs = new Object[0];
        }
    }

    public void bindAllArgsAsStrings(String[] bindArgs) {
        if (bindArgs != null) {
            for (int i = 0; i < bindArgs.length; i++) {
                if (bindArgs[i] == null) {
                    bindNull(i + 1);
                } else {
                    bindString(i + 1, bindArgs[i]);
                }
            }
        }
    }

    @Override
    protected void onAllReferencesReleased() {
        clearBindings();
    }
}
