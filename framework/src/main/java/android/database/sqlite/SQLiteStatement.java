package android.database.sqlite;

import android.os.ParcelFileDescriptor;

/** Câu lệnh SQLite đã biên dịch (INSERT / UPDATE / DELETE / DDL / truy vấn đơn). */
public final class SQLiteStatement extends SQLiteProgram {
    SQLiteStatement(SQLiteDatabase db, String sql, Object[] bindArgs) {
        super(db, sql, bindArgs, null);
    }

    public void execute() {
        acquireReference();
        try {
            getDatabase().executeStatement(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    public int executeUpdateDelete() {
        acquireReference();
        try {
            return getDatabase().executeForChangedRowCount(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    public long executeInsert() {
        acquireReference();
        try {
            return getDatabase().executeForLastInsertedRowId(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    public long simpleQueryForLong() {
        acquireReference();
        try {
            return getDatabase().executeForLong(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    public String simpleQueryForString() {
        acquireReference();
        try {
            return getDatabase().executeForString(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    public ParcelFileDescriptor simpleQueryForBlobFileDescriptor() {
        return null;
    }

    @Override
    public String toString() {
        return "SQLiteProgram: " + getSql();
    }
}
