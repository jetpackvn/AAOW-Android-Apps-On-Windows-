package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

/** Bộ xử lý lỗi mặc định khi tệp SQLite bị hỏng: xóa tệp lỗi để tạo mới. */
public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {
    private static final String TAG = "DefaultDatabaseErrorHandler";

    @Override
    public void onCorruption(SQLiteDatabase dbObj) {
        if (dbObj == null) return;
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath());
        if (!dbObj.isOpen()) {
            deleteDatabaseFile(dbObj.getPath());
            return;
        }
        try {
            dbObj.close();
        } catch (Throwable ignored) {
        } finally {
            deleteDatabaseFile(dbObj.getPath());
        }
    }

    private static void deleteDatabaseFile(String fileName) {
        if (fileName == null || ":memory:".equalsIgnoreCase(fileName)) return;
        SQLiteDatabase.deleteDatabase(new File(fileName));
    }
}
