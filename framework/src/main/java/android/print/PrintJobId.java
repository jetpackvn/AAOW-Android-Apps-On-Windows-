package android.print;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public final class PrintJobId implements Parcelable {
    private final String id;

    PrintJobId() {
        this.id = UUID.randomUUID().toString();
    }

    PrintJobId(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
    }

    public static final Creator<PrintJobId> CREATOR = new Creator<>() {
        @Override
        public PrintJobId createFromParcel(Parcel in) { return new PrintJobId(in.readString()); }
        @Override
        public PrintJobId[] newArray(int size) { return new PrintJobId[size]; }
    };
}
