package android.database;

/** Cursor trừu tượng đọc dữ liệu trực tiếp từ CursorWindow. */
public abstract class AbstractWindowedCursor extends AbstractCursor {
    protected CursorWindow mWindow;

    @Override
    public byte[] getBlob(int columnIndex) {
        checkPosition();
        return mWindow.getBlob(mPos, columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        checkPosition();
        return mWindow.getString(mPos, columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        checkPosition();
        mWindow.copyStringToBuffer(mPos, columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex) {
        checkPosition();
        return mWindow.getShort(mPos, columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        checkPosition();
        return mWindow.getInt(mPos, columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        checkPosition();
        return mWindow.getLong(mPos, columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        checkPosition();
        return mWindow.getFloat(mPos, columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        checkPosition();
        return mWindow.getDouble(mPos, columnIndex);
    }

    public boolean isBlob(int columnIndex) {
        return getType(columnIndex) == FIELD_TYPE_BLOB;
    }

    public boolean isString(int columnIndex) {
        return getType(columnIndex) == FIELD_TYPE_STRING;
    }

    public boolean isLong(int columnIndex) {
        return getType(columnIndex) == FIELD_TYPE_INTEGER;
    }

    public boolean isFloat(int columnIndex) {
        return getType(columnIndex) == FIELD_TYPE_FLOAT;
    }

    @Override
    public boolean isNull(int columnIndex) {
        checkPosition();
        return mWindow.isNull(mPos, columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        checkPosition();
        return mWindow.getType(mPos, columnIndex);
    }

    @Override
    protected void checkPosition() {
        super.checkPosition();
        if (mWindow == null) {
            throw new IllegalStateException("Attempting to access a closed CursorWindow. Most probable cause: cursor is not positioned or closed.");
        }
    }

    @Override
    public CursorWindow getWindow() {
        return mWindow;
    }

    public void setWindow(CursorWindow window) {
        if (window != mWindow) {
            closeWindow();
            mWindow = window;
        }
    }

    public boolean hasWindow() {
        return mWindow != null;
    }

    protected void closeWindow() {
        if (mWindow != null) {
            mWindow.close();
            mWindow = null;
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        closeWindow();
    }

    @Override
    public void close() {
        super.close();
        closeWindow();
    }
}
