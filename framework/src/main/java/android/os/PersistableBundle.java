package android.os;

public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable {
    public static final PersistableBundle EMPTY = new PersistableBundle();

    public PersistableBundle() {
    }

    public PersistableBundle(int capacity) {
    }

    public PersistableBundle(PersistableBundle b) {
        if (b != null) map.putAll(b.map);
    }

    public void putPersistableBundle(String key, PersistableBundle value) {
        map.put(key, value);
    }

    public PersistableBundle getPersistableBundle(String key) {
        Object o = map.get(key);
        return o instanceof PersistableBundle pb ? pb : null;
    }

    @Override
    public Object clone() {
        return new PersistableBundle(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeValue(this);
    }

    public static final Creator<PersistableBundle> CREATOR = new Creator<>() {
        @Override
        public PersistableBundle createFromParcel(Parcel in) {
            Object o = in.readValue(null);
            return o instanceof PersistableBundle pb ? pb : new PersistableBundle();
        }

        @Override
        public PersistableBundle[] newArray(int size) {
            return new PersistableBundle[size];
        }
    };
}
