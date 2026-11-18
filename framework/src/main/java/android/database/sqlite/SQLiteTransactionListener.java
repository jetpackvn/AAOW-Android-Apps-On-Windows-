package android.database.sqlite;

/** Lắng nghe các sự kiện begin / commit / rollback của transaction SQLite. */
public interface SQLiteTransactionListener {
    void onBegin();

    void onCommit();

    void onRollback();
}
