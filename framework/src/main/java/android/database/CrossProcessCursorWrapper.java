package android.database;

public class CrossProcessCursorWrapper extends CursorWrapper implements CrossProcessCursor {
    public CrossProcessCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (mCursor instanceof CrossProcessCursor cpc) {
            cpc.fillWindow(position, window);
        }
    }

    @Override
    public CursorWindow getWindow() {
        return mCursor instanceof CrossProcessCursor cpc ? cpc.getWindow() : null;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return !(mCursor instanceof CrossProcessCursor cpc) || cpc.onMove(oldPosition, newPosition);
    }
}
