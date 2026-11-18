package android.database.sqlite;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.util.Log;

import java.io.File;

/** Trình quản lý khởi tạo, nâng cấp và hạ cấp schema cơ sở dữ liệu SQLite. */
public abstract class SQLiteOpenHelper implements AutoCloseable {
    private static final String TAG = "SQLiteOpenHelper";

    private final Context context;
    private final String name;
    private final int newVersion;
    private final SQLiteDatabase.OpenParams.Builder openParamsBuilder;
    private SQLiteDatabase database;
    private boolean isInitializing;

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version,
                            DatabaseErrorHandler errorHandler) {
        this(context, name, version, new SQLiteDatabase.OpenParams.Builder()
                .setCursorFactory(factory)
                .setErrorHandler(errorHandler != null ? errorHandler : new DefaultDatabaseErrorHandler())
                .build());
    }

    public SQLiteOpenHelper(Context context, String name, int version, SQLiteDatabase.OpenParams openParams) {
        if (version < 1) throw new IllegalArgumentException("Version must be >= 1, was " + version);
        this.context = context;
        this.name = name;
        this.newVersion = version;
        this.openParamsBuilder = new SQLiteDatabase.OpenParams.Builder(openParams);
    }

    public String getDatabaseName() {
        return name;
    }

    public void setWriteAheadLoggingEnabled(boolean enabled) {
        synchronized (this) {
            if (enabled) {
                openParamsBuilder.addOpenFlags(SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
            } else {
                openParamsBuilder.removeOpenFlags(SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
            }
            if (database != null && database.isOpen() && !database.isReadOnly()) {
                if (enabled) {
                    database.enableWriteAheadLogging();
                } else {
                    database.disableWriteAheadLogging();
                }
            }
        }
    }

    public void setLookasideConfig(int slotSize, int slotCount) {
        synchronized (this) {
            openParamsBuilder.setLookasideConfig(slotSize, slotCount);
        }
    }

    public void setOpenParams(SQLiteDatabase.OpenParams openParams) {
        synchronized (this) {
            if (openParams == null) throw new IllegalArgumentException("openParams cannot be null");
            openParamsBuilder.setOpenFlags(openParams.getOpenFlags())
                    .setCursorFactory(openParams.getCursorFactory())
                    .setErrorHandler(openParams.getErrorHandler())
                    .setJournalMode(openParams.getJournalMode())
                    .setSynchronousMode(openParams.getSynchronousMode())
                    .setIdleConnectionTimeout(openParams.getIdleConnectionTimeout())
                    .setLookasideConfig(openParams.getLookasideSlotSize(), openParams.getLookasideSlotCount());
        }
    }

    public void setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
        synchronized (this) {
            openParamsBuilder.setIdleConnectionTimeout(idleConnectionTimeoutMs);
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        synchronized (this) {
            return getDatabaseLocked(true);
        }
    }

    public SQLiteDatabase getReadableDatabase() {
        synchronized (this) {
            return getDatabaseLocked(false);
        }
    }

    private SQLiteDatabase getDatabaseLocked(boolean writable) {
        if (database != null) {
            if (!database.isOpen()) {
                database = null;
            } else if (!writable || !database.isReadOnly()) {
                return database;
            }
        }
        if (isInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }

        SQLiteDatabase db = database;
        try {
            isInitializing = true;
            if (db == null) {
                SQLiteDatabase.OpenParams params = openParamsBuilder
                        .addOpenFlags(SQLiteDatabase.CREATE_IF_NECESSARY)
                        .build();
                if (name == null) {
                    db = SQLiteDatabase.createInMemory(params);
                } else {
                    File dbPath = context != null ? context.getDatabasePath(name) : new File(name);
                    db = SQLiteDatabase.openDatabase(dbPath, params);
                }
            }

            onConfigure(db);
            int version = db.getVersion();
            if (version != newVersion) {
                if (db.isReadOnly()) {
                    throw new SQLiteException("Can't upgrade read-only database from version " + version + " to " + newVersion + ": " + name);
                }
                db.beginTransaction();
                try {
                    if (version == 0) {
                        onCreate(db);
                    } else {
                        if (version > newVersion) {
                            onDowngrade(db, version, newVersion);
                        } else {
                            onUpgrade(db, version, newVersion);
                        }
                    }
                    db.setVersion(newVersion);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            onOpen(db);
            if (db.isReadOnly()) {
                Log.w(TAG, "Opened " + name + " in read-only mode");
            }
            database = db;
            return db;
        } finally {
            isInitializing = false;
            if (db != null && db != database) {
                db.close();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (isInitializing) throw new IllegalStateException("Closed during initialization");
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }

    public void onConfigure(SQLiteDatabase db) {
    }

    public abstract void onCreate(SQLiteDatabase db);

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Can't downgrade database from version " + oldVersion + " to " + newVersion);
    }

    public void onOpen(SQLiteDatabase db) {
    }
}
