package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class AbsSavedState implements Parcelable {
    public static final AbsSavedState EMPTY_STATE = new AbsSavedState() {
    };

    private final Parcelable superState;

    private AbsSavedState() {
        this.superState = null;
    }

    protected AbsSavedState(Parcelable superState) {
        if (superState == null) {
            throw new IllegalArgumentException("superState must not be null");
        }
        this.superState = superState != EMPTY_STATE ? superState : null;
    }

    protected AbsSavedState(Parcel source) {
        this(source, null);
    }

    protected AbsSavedState(Parcel source, ClassLoader loader) {
        Parcelable superState = source != null ? source.readParcelable(loader) : null;
        this.superState = superState != null ? superState : EMPTY_STATE;
    }

    public final Parcelable getSuperState() {
        return superState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) dest.writeParcelable(superState, flags);
    }
}
