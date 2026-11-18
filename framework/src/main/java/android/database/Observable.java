package android.database;

import java.util.ArrayList;

public abstract class Observable<T> {
    protected final ArrayList<T> mObservers = new ArrayList<>();

    public void registerObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized (mObservers) {
            if (!mObservers.contains(observer)) {
                mObservers.add(observer);
            }
        }
    }

    public void unregisterObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized (mObservers) {
            mObservers.remove(observer);
        }
    }

    public void unregisterAll() {
        synchronized (mObservers) {
            mObservers.clear();
        }
    }
}
