package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.CancellationSignal;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Bộ dựng câu truy vấn SQLiteQueryBuilder tương thích Android SDK. */
public class SQLiteQueryBuilder {
    private Map<String, String> projectionMap = null;
    private String tables = "";
    private StringBuilder whereClause = null;
    private boolean distinct = false;
    private SQLiteDatabase.CursorFactory factory = null;
    private boolean strict = false;
    private boolean strictColumns = false;
    private boolean strictGrammar = false;
    private Collection<Pattern> projectionGreylist = null;

    public SQLiteQueryBuilder() {
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public String getTables() {
        return tables;
    }

    public void setTables(String inTables) {
        this.tables = inTables != null ? inTables : "";
    }

    public void appendWhere(CharSequence inWhere) {
        if (whereClause == null) {
            whereClause = new StringBuilder(inWhere.length() + 16);
        }
        whereClause.append(inWhere);
    }

    public void appendWhereEscapeString(String inWhere) {
        if (whereClause == null) {
            whereClause = new StringBuilder(inWhere.length() + 16);
        }
        DatabaseUtils.appendEscapedSQLString(whereClause, inWhere);
    }

    public void appendWhereStandalone(CharSequence inWhere) {
        if (whereClause == null) {
            whereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (whereClause.length() > 0) {
            whereClause.append(" AND ");
        }
        whereClause.append('(').append(inWhere).append(')');
    }

    public void setProjectionMap(Map<String, String> columnMap) {
        this.projectionMap = columnMap;
    }

    public Map<String, String> getProjectionMap() {
        return projectionMap;
    }

    public void setProjectionGreylist(Collection<Pattern> projectionGreylist) {
        this.projectionGreylist = projectionGreylist;
    }

    public Collection<Pattern> getProjectionGreylist() {
        return projectionGreylist;
    }

    public void setCursorFactory(SQLiteDatabase.CursorFactory factory) {
        this.factory = factory;
    }

    public SQLiteDatabase.CursorFactory getCursorFactory() {
        return factory;
    }

    public void setStrict(boolean flag) {
        this.strict = flag;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrictColumns(boolean flag) {
        this.strictColumns = flag;
    }

    public boolean isStrictColumns() {
        return strictColumns;
    }

    public void setStrictGrammar(boolean flag) {
        this.strictGrammar = flag;
    }

    public boolean isStrictGrammar() {
        return strictGrammar;
    }

    public static String buildQueryString(boolean distinct, String tables, String[] columns, String where,
                                          String groupBy, String having, String orderBy, String limit) {
        if (TextUtils.isEmpty(groupBy) && !TextUtils.isEmpty(having)) {
            throw new IllegalArgumentException("HAVING clauses are only permitted when using a groupBy clause");
        }
        StringBuilder query = new StringBuilder(120);
        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ");
        query.append(tables);
        appendClause(query, " WHERE ", where);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!TextUtils.isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;
        for (int i = 0; i < n; i++) {
            String column = columns[i];
            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, null, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder, String limit) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, limit, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder, String limit,
                        CancellationSignal cancellationSignal) {
        if (tables == null) return null;
        String sql = buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
        return db.rawQueryWithFactory(factory, sql, selectionArgs, SQLiteDatabase.findEditTable(tables), cancellationSignal);
    }

    public long insert(SQLiteDatabase db, ContentValues values) {
        return db.insert(tables, null, values);
    }

    public int update(SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs) {
        String sqlWhere = combineWhere(selection);
        return db.update(tables, values, sqlWhere, selectionArgs);
    }

    public int delete(SQLiteDatabase db, String selection, String[] selectionArgs) {
        String sqlWhere = combineWhere(selection);
        return db.delete(tables, sqlWhere, selectionArgs);
    }

    private String combineWhere(String selection) {
        boolean hasUserWhere = whereClause != null && whereClause.length() > 0;
        if (!hasUserWhere) return selection;
        if (TextUtils.isEmpty(selection)) return whereClause.toString();
        return "(" + whereClause + ") AND (" + selection + ")";
    }

    public String buildQuery(String[] projectionIn, String selection, String groupBy, String having,
                             String sortOrder, String limit) {
        String[] projection = computeProjection(projectionIn);
        String where = combineWhere(selection);
        return buildQueryString(distinct, tables, projection, where, groupBy, having, sortOrder, limit);
    }

    @Deprecated
    public String buildQuery(String[] projectionIn, String selection, String[] selectionArgs, String groupBy,
                             String having, String sortOrder, String limit) {
        return buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
    }

    public String buildUnionSubQuery(String typeDiscriminatorColumn, String[] unionColumns,
                                     Set<String> columnsPresentInTable, int computedColumnsOffset,
                                     String typeDiscriminatorValue, String selection,
                                     String groupBy, String having) {
        int unionColumnsCount = unionColumns.length;
        String[] projectionIn = new String[unionColumnsCount];
        for (int i = 0; i < unionColumnsCount; i++) {
            String unionColumn = unionColumns[i];
            if (unionColumn.equals(typeDiscriminatorColumn)) {
                projectionIn[i] = "'" + typeDiscriminatorValue + "' AS " + typeDiscriminatorColumn;
            } else if (i <= computedColumnsOffset || (columnsPresentInTable != null && columnsPresentInTable.contains(unionColumn))) {
                projectionIn[i] = unionColumn;
            } else {
                projectionIn[i] = "NULL AS " + unionColumn;
            }
        }
        return buildQuery(projectionIn, selection, groupBy, having, null, null);
    }

    @Deprecated
    public String buildUnionSubQuery(String typeDiscriminatorColumn, String[] unionColumns,
                                     Set<String> columnsPresentInTable, int computedColumnsOffset,
                                     String typeDiscriminatorValue, String selection, String[] selectionArgs,
                                     String groupBy, String having) {
        return buildUnionSubQuery(typeDiscriminatorColumn, unionColumns, columnsPresentInTable,
                computedColumnsOffset, typeDiscriminatorValue, selection, groupBy, having);
    }

    public String buildUnionQuery(String[] subQueries, String sortOrder, String limit) {
        StringBuilder query = new StringBuilder(128);
        int subCount = subQueries.length;
        String unionOperator = distinct ? " UNION " : " UNION ALL ";
        for (int i = 0; i < subCount; i++) {
            if (i > 0) query.append(unionOperator);
            query.append(subQueries[i]);
        }
        appendClause(query, " ORDER BY ", sortOrder);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    private String[] computeProjection(String[] projectionIn) {
        if (projectionIn != null && projectionIn.length > 0) {
            if (projectionMap != null) {
                String[] projection = new String[projectionIn.length];
                for (int i = 0; i < projectionIn.length; i++) {
                    String userColumn = projectionIn[i];
                    String column = projectionMap.get(userColumn);
                    if (column != null) {
                        projection[i] = column;
                    } else if (!strict && (userColumn.contains(" AS ") || userColumn.contains(" as "))) {
                        projection[i] = userColumn;
                    } else {
                        throw new IllegalArgumentException("Invalid column " + userColumn);
                    }
                }
                return projection;
            }
            return projectionIn;
        } else if (projectionMap != null) {
            ArrayList<String> list = new ArrayList<>();
            for (Map.Entry<String, String> entry : projectionMap.entrySet()) {
                if (!"_count".equals(entry.getKey())) {
                    list.add(entry.getValue());
                }
            }
            return list.toArray(new String[0]);
        }
        return null;
    }
}
