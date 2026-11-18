package android.print;

import android.os.Parcel;
import android.os.Parcelable;

public final class PrintJobInfo implements Parcelable {
    public static final int STATE_CREATED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_STARTED = 3;
    public static final int STATE_BLOCKED = 4;
    public static final int STATE_COMPLETED = 5;
    public static final int STATE_FAILED = 6;
    public static final int STATE_CANCELED = 7;

    private final PrintJobId id;
    private final String label;
    private final PrintAttributes attributes;
    private int state = STATE_COMPLETED;
    private final long creationTime = System.currentTimeMillis();

    PrintJobInfo(PrintJobId id, String label, PrintAttributes attributes) {
        this.id = id;
        this.label = label;
        this.attributes = attributes != null ? attributes : new PrintAttributes();
    }

    public PrintJobId getId() { return id; }
    public String getLabel() { return label; }
    public int getState() { return state; }
    public long getCreationTime() { return creationTime; }
    public int getCopies() { return 1; }
    public PageRange[] getPages() { return new PageRange[]{PageRange.ALL_PAGES}; }
    public PrintAttributes getAttributes() { return attributes; }

    void setState(int state) { this.state = state; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(id, flags);
        dest.writeString(label);
        dest.writeInt(state);
    }

    public static final Creator<PrintJobInfo> CREATOR = new Creator<>() {
        @Override
        public PrintJobInfo createFromParcel(Parcel in) {
            PrintJobId id = in.readParcelable(null);
            String lbl = in.readString();
            int st = in.readInt();
            PrintJobInfo info = new PrintJobInfo(id, lbl, new PrintAttributes());
            info.setState(st);
            return info;
        }

        @Override
        public PrintJobInfo[] newArray(int size) { return new PrintJobInfo[size]; }
    };
}
