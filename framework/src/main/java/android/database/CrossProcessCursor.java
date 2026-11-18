package android.database;

/** Cursor hỗ trợ điền dữ liệu vào CursorWindow. */
public interface CrossProcessCursor extends Cursor {
    CursorWindow getWindow();

    void fillWindow(int position, CursorWindow window);

    boolean onMove(int oldPosition, int newPosition);
}
