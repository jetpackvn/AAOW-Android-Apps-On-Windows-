package android.database;

import android.database.sqlite.SQLiteDatabase;

/** Giao diện xử lý khi phát hiện cơ sở dữ liệu SQLite bị hỏng. */
public interface DatabaseErrorHandler {
    void onCorruption(SQLiteDatabase dbObj);
}
