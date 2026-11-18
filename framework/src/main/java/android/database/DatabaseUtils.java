package android.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteProgram;
import android.database.sqlite.SQLiteStatement;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import java.io.PrintStream;
import java.util.Locale;

/** Tiện ích xử lý SQL, escape chuỗi và chuyển đổi Cursor <-> ContentValues. */
public class DatabaseUtils {
    public static final int STATEMENT_SELECT = 1;
    public static final int STATEMENT_UPDATE = 2;
    public static final int STATEMENT_ATTACH = 3;
    public static final int STATEMENT_BEGIN = 4;
    public static final int STATEMENT_COMMIT = 5;
    public static final int STATEMENT_ABORT = 6;
    public static final int STATEMENT_PRAGMA = 7;
    public static final int STATEMENT_DDL = 8;
    public static final int STATEMENT_UNPREPARED = 9;
    public static final int STATEMENT_OTHER = 99;

    public DatabaseUtils() {
    }

    public static void appendEscapedSQLString(StringBuilder sb, String sqlString) {
        sb.append('\'');
        if (sqlString.indexOf('\'') != -1) {
            int length = sqlString.length();
            for (int i = 0; i < length; i++) {
                char c = sqlString.charAt(i);
                if (c == '\'') sb.append('\'');
                sb.append(c);
            }
        } else {
            sb.append(sqlString);
        }
        sb.append('\'');
    }

    public static String sqlEscapeString(String value) {
        StringBuilder sb = new StringBuilder();
        appendEscapedSQLString(sb, value);
        return sb.toString();
    }

    public static void appendValueToSql(StringBuilder sql, Object value) {
        if (value == null) {
            sql.append("NULL");
        } else if (value instanceof Boolean b) {
            sql.append(b ? '1' : '0');
        } else if (value instanceof Number) {
            sql.append(value);
        } else {
            appendEscapedSQLString(sql, value.toString());
        }
    }

    public static String concatenateWhere(String a, String b) {
        if (TextUtils.isEmpty(a)) return b;
        if (TextUtils.isEmpty(b)) return a;
        return "(" + a + ") AND (" + b + ")";
    }

    public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
        if (originalValues == null || originalValues.length == 0) {
            return newValues;
        }
        if (newValues == null || newValues.length == 0) {
            return originalValues;
        }
        String[] result = new String[originalValues.length + newValues.length];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length);
        System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
        return result;
    }

    public static void bindObjectToProgram(SQLiteProgram prog, int index, Object value) {
        if (value == null) {
            prog.bindNull(index);
        } else if (value instanceof Double || value instanceof Float) {
            prog.bindDouble(index, ((Number) value).doubleValue());
        } else if (value instanceof Number) {
            prog.bindLong(index, ((Number) value).longValue());
        } else if (value instanceof Boolean b) {
            prog.bindLong(index, b ? 1 : 0);
        } else if (value instanceof byte[] bytes) {
            prog.bindBlob(index, bytes);
        } else {
            prog.bindString(index, value.toString());
        }
    }

    public static int getSqlStatementType(String sql) {
        if (sql == null) return STATEMENT_OTHER;
        String trimmed = sql.trim();
        if (trimmed.length() < 3) return STATEMENT_OTHER;
        String prefix = trimmed.substring(0, 3).toUpperCase(Locale.ROOT);
        return switch (prefix) {
            case "SEL" -> STATEMENT_SELECT;
            case "INS", "UPD", "REP", "DEL" -> STATEMENT_UPDATE;
            case "ATT" -> STATEMENT_ATTACH;
            case "COM", "END" -> STATEMENT_COMMIT;
            case "ROL" -> STATEMENT_ABORT;
            case "BEG" -> STATEMENT_BEGIN;
            case "PRA" -> STATEMENT_PRAGMA;
            case "CRE", "DRO", "ALT" -> STATEMENT_DDL;
            case "ANA", "DET" -> STATEMENT_UNPREPARED;
            default -> STATEMENT_OTHER;
        };
    }

    public static long longForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try (SQLiteStatement prog = db.compileStatement(query)) {
            if (selectionArgs != null) prog.bindAllArgsAsStrings(selectionArgs);
            return prog.simpleQueryForLong();
        }
    }

    public static long longForQuery(SQLiteStatement prog, String[] selectionArgs) {
        if (selectionArgs != null) prog.bindAllArgsAsStrings(selectionArgs);
        return prog.simpleQueryForLong();
    }

    public static String stringForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try (SQLiteStatement prog = db.compileStatement(query)) {
            if (selectionArgs != null) prog.bindAllArgsAsStrings(selectionArgs);
            return prog.simpleQueryForString();
        }
    }

    public static String stringForQuery(SQLiteStatement prog, String[] selectionArgs) {
        if (selectionArgs != null) prog.bindAllArgsAsStrings(selectionArgs);
        return prog.simpleQueryForString();
    }

    public static ParcelFileDescriptor blobFileDescriptorForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        return null;
    }

    public static ParcelFileDescriptor blobFileDescriptorForQuery(SQLiteStatement prog, String[] selectionArgs) {
        return null;
    }

    public static long queryNumEntries(SQLiteDatabase db, String table) {
        return queryNumEntries(db, table, null, null);
    }

    public static long queryNumEntries(SQLiteDatabase db, String table, String selection) {
        return queryNumEntries(db, table, selection, null);
    }

    public static long queryNumEntries(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
        String s = !TextUtils.isEmpty(selection) ? " WHERE " + selection : "";
        return longForQuery(db, "SELECT COUNT(*) FROM " + table + s, selectionArgs);
    }

    public static void createDbFromSqlStatements(Context context, String dbName, int dbVersion, String sqlStatements) {
        SQLiteDatabase db = context.openOrCreateDatabase(dbName, 0, null);
        for (String statement : TextUtils.split(sqlStatements, ";\n")) {
            if (!TextUtils.isEmpty(statement.trim())) {
                db.execSQL(statement);
            }
        }
        db.setVersion(dbVersion);
        db.close();
    }

    public static void cursorStringToContentValues(Cursor cursor, String field, ContentValues values) {
        cursorStringToContentValues(cursor, field, values, field);
    }

    public static void cursorStringToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        values.put(key, cursor.getString(cursor.getColumnIndexOrThrow(field)));
    }

    public static void cursorIntToContentValues(Cursor cursor, String field, ContentValues values) {
        cursorIntToContentValues(cursor, field, values, field);
    }

    public static void cursorIntToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        int col = cursor.getColumnIndex(field);
        if (col >= 0 && !cursor.isNull(col)) values.put(key, cursor.getInt(col));
        else values.putNull(key);
    }

    public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values) {
        cursorLongToContentValues(cursor, field, values, field);
    }

    public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        int col = cursor.getColumnIndex(field);
        if (col >= 0 && !cursor.isNull(col)) values.put(key, cursor.getLong(col));
        else values.putNull(key);
    }

    public static void cursorDoubleToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        int col = cursor.getColumnIndex(field);
        if (col >= 0 && !cursor.isNull(col)) values.put(key, cursor.getDouble(col));
        else values.putNull(key);
    }

    public static void cursorDoubleToCursorValues(Cursor cursor, String field, ContentValues values) {
        cursorDoubleToContentValues(cursor, field, values, field);
    }

    public static void cursorStringToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getString(index));
    }

    public static void cursorLongToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getLong(index));
    }

    public static void cursorShortToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getShort(index));
    }

    public static void cursorIntToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getInt(index));
    }

    public static void cursorFloatToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getFloat(index));
    }

    public static void cursorDoubleToContentValuesIfPresent(Cursor cursor, ContentValues values, String column) {
        int index = cursor.getColumnIndex(column);
        if (index != -1 && !cursor.isNull(index)) values.put(column, cursor.getDouble(index));
    }

    public static void cursorRowToContentValues(Cursor cursor, ContentValues values) {
        String[] columns = cursor.getColumnNames();
        for (int i = 0; i < columns.length; i++) {
            if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                values.put(columns[i], cursor.getBlob(i));
            } else {
                values.put(columns[i], cursor.getString(i));
            }
        }
    }

    public static void dumpCursor(Cursor cursor) {
        dumpCursor(cursor, System.out);
    }

    public static void dumpCursor(Cursor cursor, PrintStream stream) {
        stream.println(dumpCursorToString(cursor));
    }

    public static void dumpCursor(Cursor cursor, StringBuilder sb) {
        sb.append(dumpCursorToString(cursor));
    }

    public static String dumpCursorToString(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>>>> Dumping cursor ").append(cursor).append('\n');
        if (cursor != null) {
            int startPos = cursor.getPosition();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                dumpCurrentRow(cursor, sb);
            }
            cursor.moveToPosition(startPos);
        }
        sb.append("<<<<<\n");
        return sb.toString();
    }

    public static void dumpCurrentRow(Cursor cursor) {
        dumpCurrentRow(cursor, System.out);
    }

    public static void dumpCurrentRow(Cursor cursor, PrintStream stream) {
        stream.println(dumpCurrentRowToString(cursor));
    }

    public static void dumpCurrentRow(Cursor cursor, StringBuilder sb) {
        sb.append(dumpCurrentRowToString(cursor));
    }

    public static String dumpCurrentRowToString(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        String[] cols = cursor.getColumnNames();
        sb.append(cursor.getPosition()).append(" {\n");
        for (int i = 0; i < cols.length; i++) {
            String value;
            try {
                value = cursor.getString(i);
            } catch (Exception e) {
                value = "<unprintable>";
            }
            sb.append("   ").append(cols[i]).append('=').append(value).append('\n');
        }
        sb.append("}\n");
        return sb.toString();
    }

    public static String getCollationKey(String name) {
        return name != null ? name.toLowerCase(Locale.ROOT) : "";
    }

    public static String getHexCollationKey(String name) {
        return getCollationKey(name);
    }

    public static void writeExceptionToParcel(Parcel reply, Exception e) {
        reply.writeInt(0);
    }

    public static void readExceptionFromParcel(Parcel reply) {
        reply.readException();
    }

    public static void readExceptionWithFileNotFoundExceptionFromParcel(Parcel reply) throws java.io.FileNotFoundException {
        reply.readException();
    }

    public static void readExceptionWithOperationApplicationExceptionFromParcel(Parcel reply) throws OperationApplicationException {
        reply.readException();
    }
}
