package android.database.sqlite;

import android.database.CursorWindow;
import android.os.CancellationSignal;

/** Đại diện câu truy vấn SELECT SQLite kèm tham số bind và hỗ trợ điền CursorWindow. */
public final class SQLiteQuery extends SQLiteProgram {
    SQLiteQuery(SQLiteDatabase db, String query, CancellationSignal cancellationSignal) {
        super(db, query, null, cancellationSignal);
    }

    String[] fillWindow(CursorWindow window, int startPos, int requiredPos, boolean countAllRows) {
        acquireReference();
        try {
            window.acquireReference();
            try {
                return getDatabase().executeForCursorWindow(
                        getSql(), getBindArgs(), window, startPos, getCancellationSignal());
            } finally {
                window.releaseReference();
            }
        } finally {
            releaseReference();
        }
    }

    @Override
    public String toString() {
        return "SQLiteQuery: " + getSql();
    }
}
