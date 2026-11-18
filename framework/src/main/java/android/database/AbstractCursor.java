package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Cài đặt cơ sở của android.database.Cursor / CrossProcessCursor. */
public abstract class AbstractCursor implements CrossProcessCursor {
    protected int mPos = -1;
    protected boolean mClosed = false;
    protected ContentResolver mContentResolver;

    private Bundle mExtras = Bundle.EMPTY;
    private final List<Uri> mNotifyUris = new ArrayList<>();
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private final List<ContentObserver> mContentObservers = new CopyOnWriteArrayList<>();

    public AbstractCursor() {
    }

    @Override
    public abstract int getCount();

    @Override
    public abstract String[] getColumnNames();

    @Override
    public abstract String getString(int column);

    @Override
    public abstract short getShort(int column);

    @Override
    public abstract int getInt(int column);

    @Override
    public abstract long getLong(int column);

    @Override
    public abstract float getFloat(int column);

    @Override
    public abstract double getDouble(int column);

    @Override
    public abstract boolean isNull(int column);

    @Override
    public int getType(int column) {
        return isNull(column) ? FIELD_TYPE_NULL : FIELD_TYPE_STRING;
    }

    @Override
    public byte[] getBlob(int column) {
        throw new UnsupportedOperationException("getBlob is not supported");
    }

    @Override
    public CursorWindow getWindow() {
        return null;
    }

    @Override
    public int getColumnCount() {
        String[] cols = getColumnNames();
        return cols != null ? cols.length : 0;
    }

    @Override
    public void deactivate() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    public boolean requery() {
        mDataSetObservable.notifyChanged();
        return true;
    }

    @Override
    public boolean isClosed() {
        return mClosed;
    }

    @Override
    public void close() {
        mClosed = true;
        mContentObservers.clear();
        deactivate();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        if (buffer == null) return;
        String result = getString(columnIndex);
        if (result == null) {
            buffer.sizeCopied = 0;
            return;
        }
        char[] chars = result.toCharArray();
        if (buffer.data == null || buffer.data.length < chars.length) {
            buffer.data = chars;
        } else {
            System.arraycopy(chars, 0, buffer.data, 0, chars.length);
        }
        buffer.sizeCopied = chars.length;
    }

    @Override
    public final int getPosition() {
        return mPos;
    }

    @Override
    public final boolean moveToPosition(int position) {
        final int count = getCount();
        if (position >= count) {
            mPos = count;
            return false;
        }
        if (position < 0) {
            mPos = -1;
            return false;
        }
        if (position == mPos) {
            return true;
        }
        boolean result = onMove(mPos, position);
        if (!result) {
            mPos = -1;
        } else {
            mPos = position;
        }
        return result;
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (window == null || position < 0 || position >= getCount()) {
            return;
        }
        window.acquireReference();
        try {
            int oldPos = getPosition();
            int numColumns = getColumnCount();
            window.clear();
            window.setStartPosition(position);
            window.setNumColumns(numColumns);
            if (moveToPosition(position)) {
                do {
                    if (!window.allocRow()) break;
                    for (int i = 0; i < numColumns; i++) {
                        int type = getType(i);
                        switch (type) {
                            case FIELD_TYPE_NULL -> window.putNull(mPos, i);
                            case FIELD_TYPE_INTEGER -> window.putLong(getLong(i), mPos, i);
                            case FIELD_TYPE_FLOAT -> window.putDouble(getDouble(i), mPos, i);
                            case FIELD_TYPE_BLOB -> window.putBlob(getBlob(i), mPos, i);
                            default -> window.putString(getString(i), mPos, i);
                        }
                    }
                } while (moveToNext());
            }
            moveToPosition(oldPos);
        } finally {
            window.releaseReference();
        }
    }

    @Override
    public final boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }

    @Override
    public final boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public final boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public final boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }

    @Override
    public final boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }

    @Override
    public final boolean isFirst() {
        return mPos == 0 && getCount() != 0;
    }

    @Override
    public final boolean isLast() {
        int cnt = getCount();
        return mPos == (cnt - 1) && cnt != 0;
    }

    @Override
    public final boolean isBeforeFirst() {
        return getCount() == 0 || mPos == -1;
    }

    @Override
    public final boolean isAfterLast() {
        return getCount() == 0 || mPos == getCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        if (columnName == null) return -1;
        String[] columnNames = getColumnNames();
        if (columnNames == null) return -1;
        int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) {
            for (int i = 0; i < columnNames.length; i++) {
                if (columnName.equalsIgnoreCase(columnNames[i])) return i;
            }
            columnName = columnName.substring(periodIndex + 1);
        }
        for (int i = 0; i < columnNames.length; i++) {
            if (columnName.equalsIgnoreCase(columnNames[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) {
        int index = getColumnIndex(columnName);
        if (index < 0) {
            throw new IllegalArgumentException("column '" + columnName + "' does not exist");
        }
        return index;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumnNames()[columnIndex];
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        if (observer != null && !mContentObservers.contains(observer)) {
            mContentObservers.add(observer);
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        if (!mClosed) {
            mContentObservers.remove(observer);
        }
    }

    protected void onChange(boolean selfChange) {
        for (ContentObserver obs : mContentObservers) {
            if (!selfChange || obs.deliverSelfNotifications()) {
                obs.dispatchChange(selfChange, mNotifyUris, 0);
            }
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (observer != null) mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) mDataSetObservable.unregisterObserver(observer);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
        synchronized (mNotifyUris) {
            mContentResolver = cr;
            mNotifyUris.clear();
            if (notifyUri != null) {
                mNotifyUris.add(notifyUri);
            }
        }
    }

    @Override
    public void setNotificationUris(ContentResolver cr, List<Uri> notifyUris) {
        synchronized (mNotifyUris) {
            mContentResolver = cr;
            mNotifyUris.clear();
            if (notifyUris != null) {
                mNotifyUris.addAll(notifyUris);
            }
        }
    }

    @Override
    public Uri getNotificationUri() {
        synchronized (mNotifyUris) {
            return mNotifyUris.isEmpty() ? null : mNotifyUris.get(0);
        }
    }

    @Override
    public List<Uri> getNotificationUris() {
        synchronized (mNotifyUris) {
            return Collections.unmodifiableList(new ArrayList<>(mNotifyUris));
        }
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public void setExtras(Bundle extras) {
        mExtras = extras != null ? extras : Bundle.EMPTY;
    }

    @Override
    public Bundle getExtras() {
        return mExtras;
    }

    @Override
    public Bundle respond(Bundle extras) {
        return Bundle.EMPTY;
    }

    protected boolean isFieldUpdated(int columnIndex) {
        return false;
    }

    protected Object getUpdatedField(int columnIndex) {
        return null;
    }

    protected void checkPosition() {
        if (mPos < 0 || mPos >= getCount()) {
            throw new IllegalStateException("Cursor position invalid: " + mPos + " (count=" + getCount() + ")");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() {
    }
}
