package android.database.sqlite;

import java.io.Closeable;

/** Lớp cơ sở quản lý đếm tham chiếu cho các đối tượng SQLite và CursorWindow. */
public abstract class SQLiteClosable implements Closeable {
    private int referenceCount = 1;

    protected abstract void onAllReferencesReleased();

    protected void onAllReferencesReleasedFromContainer() {
        onAllReferencesReleased();
    }

    public void acquireReference() {
        synchronized (this) {
            if (referenceCount <= 0) {
                throw new IllegalStateException("attempt to re-open an already-closed object: " + this);
            }
            referenceCount++;
        }
    }

    public void releaseReference() {
        boolean refCountIsZero;
        synchronized (this) {
            refCountIsZero = --referenceCount == 0;
        }
        if (refCountIsZero) {
            onAllReferencesReleased();
        }
    }

    public void releaseReferenceFromContainer() {
        boolean refCountIsZero;
        synchronized (this) {
            refCountIsZero = --referenceCount == 0;
        }
        if (refCountIsZero) {
            onAllReferencesReleasedFromContainer();
        }
    }

    @Override
    public void close() {
        releaseReference();
    }
}
