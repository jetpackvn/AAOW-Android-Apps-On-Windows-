package android.content.pm;

import android.os.Parcel;
import android.os.PatternMatcher;

public class PathPermission extends PatternMatcher {
    private final String readPermission;
    private final String writePermission;

    public PathPermission(String pattern, int type, String readPermission, String writePermission) {
        super(pattern, type);
        this.readPermission = readPermission;
        this.writePermission = writePermission;
    }

    public PathPermission(Parcel src) {
        super(src);
        this.readPermission = src.readString();
        this.writePermission = src.readString();
    }

    public String getReadPermission() {
        return readPermission;
    }

    public String getWritePermission() {
        return writePermission;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(readPermission);
        dest.writeString(writePermission);
    }

    public static final Creator<PathPermission> CREATOR = new Creator<>() {
        @Override
        public PathPermission createFromParcel(Parcel source) {
            return new PathPermission(source);
        }

        @Override
        public PathPermission[] newArray(int size) {
            return new PathPermission[size];
        }
    };
}
