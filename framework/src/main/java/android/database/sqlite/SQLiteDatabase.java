package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.database.SQLException;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Cài đặt thật android.database.sqlite.SQLiteDatabase dựa trên SQLite JDBC (org.xerial:sqlite-jdbc)
 * chạy trực tiếp trên tệp .db Windows hoặc :memory:.
 */
public final class SQLiteDatabase extends SQLiteClosable {
    private static final String TAG = "SQLiteDatabase";

    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_ROLLBACK = 1;
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_REPLACE = 5;

    private static final String[] CONFLICT_VALUES = {
            "", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "
    };

    public static final int OPEN_READWRITE = 0x00000000;
    public static final int OPEN_READONLY = 0x00000001;
    public static final int NO_LOCALIZED_COLLATORS = 0x00000010;
    public static final int CREATE_IF_NECESSARY = 0x10000000;
    public static final int ENABLE_WRITE_AHEAD_LOGGING = 0x20000000;
    public static final int MAX_SQL_CACHE_SIZE = 100;
    public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;

    public static final String JOURNAL_MODE_DELETE = "DELETE";
    public static final String JOURNAL_MODE_TRUNCATE = "TRUNCATE";
    public static final String JOURNAL_MODE_PERSIST = "PERSIST";
    public static final String JOURNAL_MODE_MEMORY = "MEMORY";
    public static final String JOURNAL_MODE_WAL = "WAL";
    public static final String JOURNAL_MODE_OFF = "OFF";

    public static final String SYNC_MODE_OFF = "OFF";
    public static final String SYNC_MODE_NORMAL = "NORMAL";
    public static final String SYNC_MODE_FULL = "FULL";
    public static final String SYNC_MODE_EXTRA = "EXTRA";

    public interface CursorFactory {
        Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query);
    }

    public static final class OpenParams {
        private final int openFlags;
        private final CursorFactory cursorFactory;
        private final DatabaseErrorHandler errorHandler;
        private final int lookasideSlotSize;
        private final int lookasideSlotCount;
        private final long idleConnectionTimeout;
        private final String journalMode;
        private final String syncMode;

        private OpenParams(int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler,
                           int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeout,
                           String journalMode, String syncMode) {
            this.openFlags = openFlags;
            this.cursorFactory = cursorFactory;
            this.errorHandler = errorHandler;
            this.lookasideSlotSize = lookasideSlotSize;
            this.lookasideSlotCount = lookasideSlotCount;
            this.idleConnectionTimeout = idleConnectionTimeout;
            this.journalMode = journalMode;
            this.syncMode = syncMode;
        }

        public int getOpenFlags() {
            return openFlags;
        }

        public CursorFactory getCursorFactory() {
            return cursorFactory;
        }

        public DatabaseErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public int getLookasideSlotSize() {
            return lookasideSlotSize;
        }

        public int getLookasideSlotCount() {
            return lookasideSlotCount;
        }

        public long getIdleConnectionTimeout() {
            return idleConnectionTimeout;
        }

        public String getJournalMode() {
            return journalMode;
        }

        public String getSynchronousMode() {
            return syncMode;
        }

        public static final class Builder {
            private int openFlags;
            private CursorFactory cursorFactory;
            private DatabaseErrorHandler errorHandler;
            private int lookasideSlotSize = -1;
            private int lookasideSlotCount = -1;
            private long idleConnectionTimeout = -1;
            private String journalMode;
            private String syncMode;

            public Builder() {
            }

            public Builder(OpenParams params) {
                if (params != null) {
                    this.openFlags = params.openFlags;
                    this.cursorFactory = params.cursorFactory;
                    this.errorHandler = params.errorHandler;
                    this.lookasideSlotSize = params.lookasideSlotSize;
                    this.lookasideSlotCount = params.lookasideSlotCount;
                    this.idleConnectionTimeout = params.idleConnectionTimeout;
                    this.journalMode = params.journalMode;
                    this.syncMode = params.syncMode;
                }
            }

            public Builder addOpenFlags(int openFlags) {
                this.openFlags |= openFlags;
                return this;
            }

            public Builder removeOpenFlags(int openFlags) {
                this.openFlags &= ~openFlags;
                return this;
            }

            public Builder setOpenFlags(int openFlags) {
                this.openFlags = openFlags;
                return this;
            }

            public Builder setCursorFactory(CursorFactory cursorFactory) {
                this.cursorFactory = cursorFactory;
                return this;
            }

            public Builder setErrorHandler(DatabaseErrorHandler errorHandler) {
                this.errorHandler = errorHandler;
                return this;
            }

            public Builder setLookasideConfig(int slotSize, int slotCount) {
                this.lookasideSlotSize = slotSize;
                this.lookasideSlotCount = slotCount;
                return this;
            }

            public Builder setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
                this.idleConnectionTimeout = idleConnectionTimeoutMs;
                return this;
            }

            public Builder setJournalMode(String journalMode) {
                this.journalMode = journalMode;
                return this;
            }

            public Builder setSynchronousMode(String syncMode) {
                this.syncMode = syncMode;
                return this;
            }

            public OpenParams build() {
                return new OpenParams(openFlags, cursorFactory, errorHandler, lookasideSlotSize,
                        lookasideSlotCount, idleConnectionTimeout, journalMode, syncMode);
            }
        }
    }

    private static final class TransactionFrame {
        final SQLiteTransactionListener listener;
        boolean successful;
        boolean childFailed;

        TransactionFrame(SQLiteTransactionListener listener) {
            this.listener = listener;
        }
    }

    private static final List<java.lang.ref.WeakReference<SQLiteDatabase>> OPEN_DATABASES =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Không nạp được driver org.sqlite.JDBC", e);
        }
    }

    public static int releaseMemory() {
        int closed = 0;
        for (java.lang.ref.WeakReference<SQLiteDatabase> ref : OPEN_DATABASES) {
            SQLiteDatabase db = ref.get();
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                    closed++;
                } catch (Throwable ignored) {
                }
            }
        }
        OPEN_DATABASES.clear();
        return closed;
    }

    private final String path;
    private final CursorFactory cursorFactory;
    private final DatabaseErrorHandler errorHandler;
    private final int openFlags;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Deque<TransactionFrame> txStack = new ArrayDeque<>();
    private Connection connection;
    private volatile boolean walEnabled;
    private long lastInsertRowId = -1;
    private long lastChangedRowCount = 0;
    private long totalChangedRowCount = 0;

    private SQLiteDatabase(String path, int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler) {
        this.path = path;
        this.openFlags = openFlags;
        this.cursorFactory = cursorFactory;
        this.errorHandler = errorHandler != null ? errorHandler : new DefaultDatabaseErrorHandler();
        openConnection();
        OPEN_DATABASES.add(new java.lang.ref.WeakReference<>(this));
    }

    private void openConnection() {
        boolean isMemory = ":memory:".equals(path);
        boolean readOnly = isReadOnly();
        if (!isMemory) {
            File file = new File(path);
            if ((openFlags & CREATE_IF_NECESSARY) != 0) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
            } else if (!file.exists()) {
                throw new SQLiteCantOpenDatabaseException("Cannot open database file (does not exist): " + path);
            }
        }
        try {
            Properties props = new Properties();
            String url = isMemory ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + new File(path).getAbsolutePath();
            connection = DriverManager.getConnection(url, props);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA busy_timeout = 5000");
                if (readOnly) {
                    st.execute("PRAGMA query_only = ON");
                }
            }
            if ((openFlags & ENABLE_WRITE_AHEAD_LOGGING) != 0 && !isMemory && !readOnly) {
                enableWriteAheadLogging();
            }
        } catch (java.sql.SQLException e) {
            throw translateSqlException("Failed to open database " + path, e);
        }
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
        return openDatabase(path, factory, flags, null);
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags, DatabaseErrorHandler errorHandler) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be null or empty");
        }
        return new SQLiteDatabase(resolveDatabasePath(path), flags, factory, errorHandler);
    }

    public static SQLiteDatabase openDatabase(File path, OpenParams openParams) {
        if (path == null) throw new IllegalArgumentException("path must not be null");
        OpenParams p = openParams != null ? openParams : new OpenParams.Builder().build();
        SQLiteDatabase db = new SQLiteDatabase(resolveDatabasePath(path.getPath()), p.getOpenFlags(), p.getCursorFactory(), p.getErrorHandler());
        if (p.getJournalMode() != null && !db.isReadOnly() && !":memory:".equals(db.path)) {
            db.execSQL("PRAGMA journal_mode = " + p.getJournalMode());
            db.walEnabled = JOURNAL_MODE_WAL.equalsIgnoreCase(p.getJournalMode());
        }
        if (p.getSynchronousMode() != null && !db.isReadOnly()) {
            db.execSQL("PRAGMA synchronous = " + p.getSynchronousMode());
        }
        return db;
    }

    private static String resolveDatabasePath(String path) {
        if (path == null || path.isEmpty() || ":memory:".equals(path)) {
            return path;
        }
        File raw = new File(path);
        if (!raw.isAbsolute() && !path.contains("/") && !path.contains("\\")) {
            try {
                android.app.Application app = android.app.ActivityThread.currentApplication();
                if (app != null && app.getBaseContext() != null) {
                    File appDb = app.getDatabasePath(path);
                    if (appDb != null) {
                        return appDb.getAbsolutePath();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return path;
    }

    public static SQLiteDatabase openOrCreateDatabase(File file, CursorFactory factory) {
        return openOrCreateDatabase(file.getPath(), factory);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, null);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler);
    }

    public static SQLiteDatabase create(CursorFactory factory) {
        return openDatabase(":memory:", factory, CREATE_IF_NECESSARY, null);
    }

    public static SQLiteDatabase createInMemory(OpenParams openParams) {
        OpenParams p = openParams != null ? openParams : new OpenParams.Builder().build();
        return openDatabase(":memory:", p.getCursorFactory(), p.getOpenFlags() | CREATE_IF_NECESSARY, p.getErrorHandler());
    }

    public static boolean deleteDatabase(File file) {
        if (file == null) return false;
        File resolved = new File(resolveDatabasePath(file.getPath()));
        boolean deleted = false;
        deleted |= resolved.delete();
        deleted |= new File(resolved.getPath() + "-journal").delete();
        deleted |= new File(resolved.getPath() + "-shm").delete();
        deleted |= new File(resolved.getPath() + "-wal").delete();
        return deleted;
    }

    public static String findEditTable(String tables) {
        if (!TextUtils.isEmpty(tables)) {
            int spacepos = tables.indexOf(' ');
            int commapos = tables.indexOf(',');
            if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                return tables.substring(0, spacepos);
            } else if (commapos > 0 && (commapos < spacepos || spacepos < 0)) {
                return tables.substring(0, commapos);
            }
            return tables;
        }
        throw new IllegalStateException("Invalid tables");
    }

    public String getPath() {
        return path;
    }

    public boolean isOpen() {
        lock.lock();
        try {
            return connection != null && !connection.isClosed();
        } catch (java.sql.SQLException e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean isReadOnly() {
        return (openFlags & OPEN_READONLY) != 0;
    }

    private void verifyOpenAndWritable() {
        if (!isOpen()) {
            throw new IllegalStateException("The database '" + path + "' is not open");
        }
        if (isReadOnly()) {
            throw new SQLiteReadOnlyDatabaseException("Attempt to write a readonly database: " + path);
        }
    }

    private void verifyOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("The database '" + path + "' is not open");
        }
    }

    @Override
    protected void onAllReferencesReleased() {
        lock.lock();
        try {
            if (connection != null) {
                try {
                    connection.close();
                } catch (java.sql.SQLException ignored) {
                }
                connection = null;
            }
        } finally {
            lock.unlock();
        }
    }

    // --------------------------------------------------------- Transactions

    public void beginTransaction() {
        beginTransactionInternal(null, false);
    }

    public void beginTransactionNonExclusive() {
        beginTransactionInternal(null, false);
    }

    public void beginTransactionReadOnly() {
        beginTransactionInternal(null, true);
    }

    public void beginTransactionWithListener(SQLiteTransactionListener listener) {
        beginTransactionInternal(listener, false);
    }

    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener listener) {
        beginTransactionInternal(listener, false);
    }

    public void beginTransactionWithListenerReadOnly(SQLiteTransactionListener listener) {
        beginTransactionInternal(listener, true);
    }

    private void beginTransactionInternal(SQLiteTransactionListener listener, boolean readOnlyTx) {
        acquireReference();
        lock.lock();
        boolean pushed = false;
        boolean startedSql = false;
        try {
            verifyOpen();
            if (txStack.isEmpty()) {
                try (Statement st = connection.createStatement()) {
                    st.execute(readOnlyTx || isReadOnly() ? "BEGIN DEFERRED" : "BEGIN IMMEDIATE");
                }
                startedSql = true;
            }
            txStack.push(new TransactionFrame(listener));
            pushed = true;
            if (listener != null) {
                listener.onBegin();
            }
        } catch (Throwable t) {
            if (pushed) {
                txStack.pop();
            }
            if (startedSql && txStack.isEmpty()) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ROLLBACK");
                } catch (Throwable ignored) {
                }
            }
            lock.unlock();
            releaseReference();
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof java.sql.SQLException se) throw translateSqlException("beginTransaction failed", se);
            throw new SQLiteException("beginTransaction failed", t);
        }
    }

    public void setTransactionSuccessful() {
        lock.lock();
        try {
            if (txStack.isEmpty()) {
                throw new IllegalStateException("No transaction pending");
            }
            TransactionFrame top = txStack.peek();
            if (top.successful) {
                throw new IllegalStateException("setTransactionSuccessful may only be called once per transaction");
            }
            top.successful = true;
        } finally {
            lock.unlock();
        }
    }

    public void endTransaction() {
        try {
            if (!lock.isHeldByCurrentThread() || txStack.isEmpty()) {
                throw new IllegalStateException("Cannot end transaction when none is active on current thread");
            }
            TransactionFrame frame = txStack.pop();
            boolean ok = frame.successful && !frame.childFailed;
            RuntimeException listenerEx = null;
            if (frame.listener != null) {
                try {
                    if (ok) frame.listener.onCommit();
                    else frame.listener.onRollback();
                } catch (RuntimeException ex) {
                    listenerEx = ex;
                    ok = false;
                }
            }
            if (!txStack.isEmpty()) {
                if (!ok) {
                    txStack.peek().childFailed = true;
                }
            } else {
                try (Statement st = connection.createStatement()) {
                    st.execute(ok ? "COMMIT" : "ROLLBACK");
                } catch (java.sql.SQLException e) {
                    throw translateSqlException("endTransaction failed", e);
                }
            }
            if (listenerEx != null) {
                throw listenerEx;
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            releaseReference();
        }
    }

    public boolean inTransaction() {
        lock.lock();
        try {
            return !txStack.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public boolean isDbLockedByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    @Deprecated
    public boolean isDbLockedByOtherThreads() {
        return lock.isLocked() && !lock.isHeldByCurrentThread();
    }

    public boolean yieldIfContended() {
        return yieldIfContendedSafely(-1);
    }

    public boolean yieldIfContendedSafely() {
        return yieldIfContendedSafely(-1);
    }

    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        if (!inTransaction() || !lock.hasQueuedThreads()) {
            return false;
        }
        if (sleepAfterYieldDelay > 0) {
            try {
                Thread.sleep(sleepAfterYieldDelay);
            } catch (InterruptedException ignored) {
            }
        }
        return true;
    }

    // --------------------------------------------------------- Version / Pragmas

    public int getVersion() {
        return (int) executeForLong("PRAGMA user_version", null);
    }

    public void setVersion(int version) {
        execSQL("PRAGMA user_version = " + version);
    }

    public boolean needUpgrade(int newVersion) {
        return newVersion > getVersion();
    }

    public boolean enableWriteAheadLogging() {
        if (isReadOnly() || ":memory:".equals(path)) {
            return false;
        }
        lock.lock();
        try {
            verifyOpen();
            String mode = executeForString("PRAGMA journal_mode=WAL", null);
            walEnabled = JOURNAL_MODE_WAL.equalsIgnoreCase(mode);
            return walEnabled;
        } finally {
            lock.unlock();
        }
    }

    public void disableWriteAheadLogging() {
        if (isReadOnly() || ":memory:".equals(path)) {
            walEnabled = false;
            return;
        }
        lock.lock();
        try {
            verifyOpen();
            executeForString("PRAGMA journal_mode=DELETE", null);
            walEnabled = false;
        } finally {
            lock.unlock();
        }
    }

    public boolean isWriteAheadLoggingEnabled() {
        return walEnabled;
    }

    public void setForeignKeyConstraintsEnabled(boolean enable) {
        execSQL("PRAGMA foreign_keys=" + (enable ? "ON" : "OFF"));
    }

    public long getMaximumSize() {
        long pageCount = executeForLong("PRAGMA max_page_count", null);
        return pageCount * getPageSize();
    }

    public long setMaximumSize(long numBytes) {
        long pageSize = getPageSize();
        long numPages = numBytes / pageSize;
        if ((numBytes % pageSize) != 0) numPages++;
        return executeForLong("PRAGMA max_page_count = " + numPages, null) * pageSize;
    }

    public long getPageSize() {
        return executeForLong("PRAGMA page_size", null);
    }

    public void setPageSize(long numBytes) {
        execSQL("PRAGMA page_size = " + numBytes);
    }

    public boolean isDatabaseIntegrityOk() {
        try {
            String res = executeForString("PRAGMA integrity_check(1)", null);
            return "ok".equalsIgnoreCase(res);
        } catch (SQLiteException e) {
            return false;
        }
    }

    public List<Pair<String, String>> getAttachedDbs() {
        List<Pair<String, String>> list = new ArrayList<>();
        try (Cursor c = rawQuery("PRAGMA database_list", null)) {
            while (c.moveToNext()) {
                list.add(new Pair<>(c.getString(1), c.getString(2)));
            }
        }
        return list;
    }

    public long getLastInsertRowId() {
        return lastInsertRowId;
    }

    public long getLastChangedRowCount() {
        return lastChangedRowCount;
    }

    public long getTotalChangedRowCount() {
        return totalChangedRowCount;
    }

    public void setLocale(Locale locale) {
    }

    public void setMaxSqlCacheSize(int cacheSize) {
    }

    @Deprecated
    public void setLockingEnabled(boolean lockingEnabled) {
    }

    public void markTableSyncable(String table, String deletedTable) {
    }

    public void markTableSyncable(String table, String foreignKey, String updateTable) {
    }

    public Map<String, String> getSyncedTables() {
        return new HashMap<>();
    }

    public void setCustomScalarFunction(String functionName, UnaryOperator<String> scalarFunction) throws SQLiteException {
    }

    public void setCustomAggregateFunction(String functionName, BinaryOperator<String> aggregateFunction) throws SQLiteException {
    }

    // --------------------------------------------------------- Execute & Compile

    public SQLiteStatement compileStatement(String sql) throws SQLException {
        acquireReference();
        try {
            verifyOpen();
            return new SQLiteStatement(this, sql, null);
        } finally {
            releaseReference();
        }
    }

    public void execSQL(String sql) throws SQLException {
        execSQL(sql, null);
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        acquireReference();
        try {
            verifyOpen();
            executeStatement(sql, bindArgs);
        } finally {
            releaseReference();
        }
    }

    public void execPerConnectionSQL(String sql, Object[] bindArgs) throws SQLException {
        execSQL(sql, bindArgs);
    }

    public void validateSql(String sql, CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        acquireReference();
        lock.lock();
        try {
            verifyOpen();
            try (PreparedStatement ps = connection.prepareStatement("EXPLAIN " + sql)) {
                ps.getMetaData();
            } catch (java.sql.SQLException e) {
                throw translateSqlException("Invalid SQL: " + sql, e);
            }
        } finally {
            lock.unlock();
            releaseReference();
        }
    }

    // --------------------------------------------------------- High-level CRUD

    public long insert(String table, String nullColumnHack, ContentValues values) {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values, e);
            return -1;
        }
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE);
    }

    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) {
        acquireReference();
        try {
            verifyOpenAndWritable();
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT");
            sql.append(conflictClause(conflictAlgorithm));
            sql.append(" INTO ");
            sql.append(table);
            sql.append('(');

            Object[] bindArgs = null;
            int size = (initialValues != null && !initialValues.isEmpty()) ? initialValues.size() : 0;
            if (size > 0) {
                bindArgs = new Object[size];
                int i = 0;
                for (Map.Entry<String, Object> entry : initialValues.valueSet()) {
                    sql.append((i > 0) ? "," : "");
                    sql.append(entry.getKey());
                    bindArgs[i++] = entry.getValue();
                }
                sql.append(')');
                sql.append(" VALUES (");
                for (i = 0; i < size; i++) {
                    sql.append((i > 0) ? ",?" : "?");
                }
            } else {
                sql.append(nullColumnHack).append(") VALUES (NULL");
            }
            sql.append(')');

            try (SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs)) {
                return statement.executeInsert();
            }
        } finally {
            releaseReference();
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        acquireReference();
        try {
            verifyOpenAndWritable();
            String sql = "DELETE FROM " + table + (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : "");
            try (SQLiteStatement statement = new SQLiteStatement(this, sql, whereArgs)) {
                return statement.executeUpdateDelete();
            }
        } finally {
            releaseReference();
        }
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE);
    }

    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs, int conflictAlgorithm) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }
        acquireReference();
        try {
            verifyOpenAndWritable();
            StringBuilder sql = new StringBuilder(120);
            sql.append("UPDATE");
            String conflict = conflictClause(conflictAlgorithm);
            sql.append(conflict.isEmpty() ? " " : conflict);
            sql.append(table);
            sql.append(" SET ");

            int setValuesSize = values.size();
            int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
            Object[] bindArgs = new Object[bindArgsSize];
            int i = 0;
            for (Map.Entry<String, Object> entry : values.valueSet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(entry.getKey());
                bindArgs[i++] = entry.getValue();
                sql.append("=?");
            }
            if (whereArgs != null) {
                for (i = setValuesSize; i < bindArgsSize; i++) {
                    bindArgs[i] = whereArgs[i - setValuesSize];
                }
            }
            if (!TextUtils.isEmpty(whereClause)) {
                sql.append(" WHERE ");
                sql.append(whereClause);
            }
            try (SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs)) {
                return statement.executeUpdateDelete();
            }
        } finally {
            releaseReference();
        }
    }

    private static String conflictClause(int conflictAlgorithm) {
        if (conflictAlgorithm >= 0 && conflictAlgorithm < CONFLICT_VALUES.length) {
            return CONFLICT_VALUES[conflictAlgorithm];
        }
        return "";
    }

    // --------------------------------------------------------- Queries

    public Cursor query(boolean distinct, String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having, String orderBy, String limit,
                        CancellationSignal cancellationSignal) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns,
                                   String selection, String[] selectionArgs, String groupBy, String having,
                                   String orderBy, String limit) {
        return queryWithFactory(cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns,
                                   String selection, String[] selectionArgs, String groupBy, String having,
                                   String orderBy, String limit, CancellationSignal cancellationSignal) {
        acquireReference();
        try {
            String sql = SQLiteQueryBuilder.buildQueryString(
                    distinct, table, columns, selection, groupBy, having, orderBy, limit);
            return rawQueryWithFactory(cursorFactory, sql, selectionArgs, findEditTable(table), cancellationSignal);
        } finally {
            releaseReference();
        }
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, null);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, cancellationSignal);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs, String editTable) {
        return rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable, null);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs,
                                      String editTable, CancellationSignal cancellationSignal) {
        acquireReference();
        try {
            verifyOpen();
            if (cancellationSignal != null) {
                cancellationSignal.throwIfCanceled();
            }
            SQLiteQuery query = new SQLiteQuery(this, sql, cancellationSignal);
            if (selectionArgs != null && selectionArgs.length > 0) {
                query.bindAllArgsAsStrings(selectionArgs);
            }
            SQLiteCursorDriver driver = new DirectCursorDriver(this, sql, editTable, query);
            CursorFactory factory = cursorFactory != null ? cursorFactory : this.cursorFactory;
            if (factory != null) {
                return factory.newCursor(this, driver, editTable, query);
            }
            return new SQLiteCursor(driver, editTable, query);
        } finally {
            releaseReference();
        }
    }

    private static final class DirectCursorDriver implements SQLiteCursorDriver {
        private final SQLiteDatabase db;
        private final String sql;
        private final String editTable;
        private final SQLiteQuery query;

        DirectCursorDriver(SQLiteDatabase db, String sql, String editTable, SQLiteQuery query) {
            this.db = db;
            this.sql = sql;
            this.editTable = editTable;
            this.query = query;
        }

        @Override
        public Cursor query(CursorFactory factory, String[] bindArgs) {
            SQLiteQuery q = new SQLiteQuery(db, sql, null);
            if (bindArgs != null) q.bindAllArgsAsStrings(bindArgs);
            return factory != null ? factory.newCursor(db, this, editTable, q) : new SQLiteCursor(this, editTable, q);
        }

        @Override
        public void cursorDeactivated() {
        }

        @Override
        public void cursorRequeried(Cursor cursor) {
        }

        @Override
        public void cursorClosed() {
        }

        @Override
        public void setBindArguments(String[] bindArgs) {
            query.clearBindings();
            if (bindArgs != null) {
                query.bindAllArgsAsStrings(bindArgs);
            }
        }
    }

    // --------------------------------------------------------- Low-level JDBC Execution

    void executeStatement(String sql, Object[] bindArgs) {
        lock.lock();
        try {
            verifyOpen();
            if (bindArgs == null || bindArgs.length == 0) {
                try (Statement st = connection.createStatement()) {
                    boolean hasRs = st.execute(sql);
                    if (hasRs) {
                        try (ResultSet rs = st.getResultSet()) {
                            while (rs != null && rs.next()) {
                                // drain
                            }
                        }
                    } else {
                        int updated = st.getUpdateCount();
                        if (updated >= 0) {
                            lastChangedRowCount = updated;
                            totalChangedRowCount += updated;
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    bindArgs(ps, bindArgs);
                    boolean hasRs = ps.execute();
                    if (hasRs) {
                        try (ResultSet rs = ps.getResultSet()) {
                            while (rs != null && rs.next()) {
                                // drain
                            }
                        }
                    } else {
                        int updated = ps.getUpdateCount();
                        if (updated >= 0) {
                            lastChangedRowCount = updated;
                            totalChangedRowCount += updated;
                        }
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL execution error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    int executeForChangedRowCount(String sql, Object[] bindArgs) {
        lock.lock();
        try {
            verifyOpenAndWritable();
            int changed;
            if (bindArgs == null || bindArgs.length == 0) {
                try (Statement st = connection.createStatement()) {
                    changed = st.executeUpdate(sql);
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    bindArgs(ps, bindArgs);
                    changed = ps.executeUpdate();
                }
            }
            lastChangedRowCount = changed;
            totalChangedRowCount += Math.max(0, changed);
            return changed;
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL update/delete error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    long executeForLastInsertedRowId(String sql, Object[] bindArgs) {
        lock.lock();
        try {
            verifyOpenAndWritable();
            int changed;
            if (bindArgs == null || bindArgs.length == 0) {
                try (Statement st = connection.createStatement()) {
                    changed = st.executeUpdate(sql);
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    bindArgs(ps, bindArgs);
                    changed = ps.executeUpdate();
                }
            }
            lastChangedRowCount = changed;
            if (changed == 0) {
                return -1L;
            }
            totalChangedRowCount += changed;
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    lastInsertRowId = rs.getLong(1);
                    return lastInsertRowId;
                }
            }
            return -1L;
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL insert error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    long executeForLong(String sql, Object[] bindArgs) {
        lock.lock();
        try {
            verifyOpen();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindArgs(ps, bindArgs);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLiteDoneException("Query returned 0 rows: " + sql);
                    }
                    return rs.getLong(1);
                }
            }
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL scalar long error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    String executeForString(String sql, Object[] bindArgs) {
        lock.lock();
        try {
            verifyOpen();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindArgs(ps, bindArgs);
                boolean hasRs = ps.execute();
                if (!hasRs) {
                    throw new SQLiteDoneException("Query returned no result set: " + sql);
                }
                try (ResultSet rs = ps.getResultSet()) {
                    if (!rs.next()) {
                        throw new SQLiteDoneException("Query returned 0 rows: " + sql);
                    }
                    return rs.getString(1);
                }
            }
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL scalar string error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    String[] executeForCursorWindow(String sql, Object[] bindArgs, CursorWindow window,
                                    int startPos, CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }
        lock.lock();
        try {
            verifyOpen();
            if (cancellationSignal != null) {
                cancellationSignal.throwIfCanceled();
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindArgs(ps, bindArgs);
                boolean hasRs = ps.execute();
                window.clear();
                window.setStartPosition(startPos);
                if (!hasRs) {
                    window.setNumColumns(0);
                    return new String[0];
                }
                try (ResultSet rs = ps.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    String[] colNames = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        String label = meta.getColumnLabel(i + 1);
                        colNames[i] = label != null && !label.isEmpty() ? label : meta.getColumnName(i + 1);
                    }
                    window.setNumColumns(colCount);
                    int rowIdx = 0;
                    while (rs.next()) {
                        if (cancellationSignal != null) {
                            cancellationSignal.throwIfCanceled();
                        }
                        if (rowIdx >= startPos) {
                            if (!window.allocRow()) break;
                            for (int c = 0; c < colCount; c++) {
                                Object val = rs.getObject(c + 1);
                                if (val == null) {
                                    window.putNull(rowIdx, c);
                                } else if (val instanceof byte[] bytes) {
                                    window.putBlob(bytes, rowIdx, c);
                                } else if (val instanceof Integer || val instanceof Long || val instanceof Short || val instanceof Byte) {
                                    window.putLong(((Number) val).longValue(), rowIdx, c);
                                } else if (val instanceof Float || val instanceof Double) {
                                    window.putDouble(((Number) val).doubleValue(), rowIdx, c);
                                } else {
                                    window.putString(val.toString(), rowIdx, c);
                                }
                            }
                        }
                        rowIdx++;
                    }
                    return colNames;
                }
            }
        } catch (java.sql.SQLException e) {
            throw translateSqlException("SQL query error: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    private static void bindArgs(PreparedStatement ps, Object[] args) throws java.sql.SQLException {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            int idx = i + 1;
            Object arg = args[i];
            if (arg == null) {
                ps.setNull(idx, Types.NULL);
            } else if (arg instanceof byte[] bytes) {
                ps.setBytes(idx, bytes);
            } else if (arg instanceof Long l) {
                ps.setLong(idx, l);
            } else if (arg instanceof Integer n) {
                ps.setInt(idx, n);
            } else if (arg instanceof Short s) {
                ps.setShort(idx, s);
            } else if (arg instanceof Byte b) {
                ps.setByte(idx, b);
            } else if (arg instanceof Double d) {
                ps.setDouble(idx, d);
            } else if (arg instanceof Float f) {
                ps.setFloat(idx, f);
            } else if (arg instanceof Boolean bool) {
                ps.setInt(idx, bool ? 1 : 0);
            } else {
                ps.setString(idx, arg.toString());
            }
        }
    }

    private SQLiteException translateSqlException(String contextMsg, java.sql.SQLException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String upper = msg.toUpperCase(Locale.ROOT);
        String full = contextMsg + ": " + msg;
        if (upper.contains("SQLITE_CONSTRAINT") || upper.contains("CONSTRAINT")) {
            return withCause(new SQLiteConstraintException(full), e);
        }
        if (upper.contains("SQLITE_CORRUPT") || upper.contains("NOT A DATABASE") || upper.contains("MALFORMED")) {
            try {
                if (errorHandler != null) errorHandler.onCorruption(this);
            } catch (Throwable ignored) {
            }
            return withCause(new SQLiteDatabaseCorruptException(full), e);
        }
        if (upper.contains("SQLITE_CANTOPEN") || upper.contains("UNABLE TO OPEN")) {
            return withCause(new SQLiteCantOpenDatabaseException(full), e);
        }
        if (upper.contains("SQLITE_READONLY") || upper.contains("READONLY")) {
            return withCause(new SQLiteReadOnlyDatabaseException(full), e);
        }
        if (upper.contains("SQLITE_FULL")) {
            return withCause(new SQLiteFullException(full), e);
        }
        if (upper.contains("SQLITE_IOERR")) {
            return withCause(new SQLiteDiskIOException(full), e);
        }
        if (upper.contains("SQLITE_ABORT")) {
            return withCause(new SQLiteAbortException(full), e);
        }
        if (upper.contains("SQLITE_MISUSE")) {
            return withCause(new SQLiteMisuseException(full), e);
        }
        return new SQLiteException(full, e);
    }

    private static <T extends SQLiteException> T withCause(T ex, Throwable cause) {
        ex.initCause(cause);
        return ex;
    }

    @Override
    public String toString() {
        return "SQLiteDatabase: " + path;
    }
}
