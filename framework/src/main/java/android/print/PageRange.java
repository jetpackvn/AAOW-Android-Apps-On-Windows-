package android.print;

import android.os.Parcel;
import android.os.Parcelable;

public final class PageRange implements Parcelable {
    public static final PageRange ALL_PAGES = new PageRange(0, Integer.MAX_VALUE);

    private final int start;
    private final int end;

    public PageRange(int start, int end) {
        if (start < 0 || end < 0 || start > end) {
            throw new IllegalArgumentException("Invalid page range");
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(start);
        dest.writeInt(end);
    }

    public static final Creator<PageRange> CREATOR = new Creator<>() {
        @Override
        public PageRange createFromParcel(Parcel in) {
            return new PageRange(in.readInt(), in.readInt());
        }

        @Override
        public PageRange[] newArray(int size) {
            return new PageRange[size];
        }
    };
}
