package android.print;

import android.os.Parcel;
import android.os.Parcelable;

public final class PrintDocumentInfo implements Parcelable {
    public static final int CONTENT_TYPE_UNKNOWN = -1;
    public static final int CONTENT_TYPE_DOCUMENT = 0;
    public static final int CONTENT_TYPE_PHOTO = 1;
    public static final int PAGE_COUNT_UNKNOWN = -1;

    private String name;
    private int contentType = CONTENT_TYPE_DOCUMENT;
    private int pageCount = PAGE_COUNT_UNKNOWN;
    private long dataSize;

    PrintDocumentInfo() {
    }

    public String getName() { return name; }
    public int getContentType() { return contentType; }
    public int getPageCount() { return pageCount; }
    public long getDataSize() { return dataSize; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(contentType);
        dest.writeInt(pageCount);
        dest.writeLong(dataSize);
    }

    public static final Creator<PrintDocumentInfo> CREATOR = new Creator<>() {
        @Override
        public PrintDocumentInfo createFromParcel(Parcel in) {
            PrintDocumentInfo info = new PrintDocumentInfo();
            info.name = in.readString();
            info.contentType = in.readInt();
            info.pageCount = in.readInt();
            info.dataSize = in.readLong();
            return info;
        }

        @Override
        public PrintDocumentInfo[] newArray(int size) {
            return new PrintDocumentInfo[size];
        }
    };

    public static final class Builder {
        private final PrintDocumentInfo info = new PrintDocumentInfo();

        public Builder(String name) {
            info.name = name;
        }

        public Builder setContentType(int type) {
            info.contentType = type;
            return this;
        }

        public Builder setPageCount(int pageCount) {
            info.pageCount = pageCount;
            return this;
        }

        public PrintDocumentInfo build() {
            return info;
        }
    }
}
