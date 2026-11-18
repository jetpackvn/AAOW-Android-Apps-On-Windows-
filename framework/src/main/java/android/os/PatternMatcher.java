package android.os;

public class PatternMatcher implements Parcelable {
    public static final int PATTERN_LITERAL = 0;
    public static final int PATTERN_PREFIX = 1;
    public static final int PATTERN_SIMPLE_GLOB = 2;
    public static final int PATTERN_ADVANCED_GLOB = 3;
    public static final int PATTERN_SUFFIX = 4;

    private final String pattern;
    private final int type;

    public PatternMatcher(String pattern, int type) {
        this.pattern = pattern;
        this.type = type;
    }

    public PatternMatcher(Parcel src) {
        this.pattern = src.readString();
        this.type = src.readInt();
    }

    public final String getPath() {
        return pattern;
    }

    public final int getType() {
        return type;
    }

    public boolean match(String str) {
        if (str == null || pattern == null) return false;
        return switch (type) {
            case PATTERN_LITERAL -> pattern.equals(str);
            case PATTERN_PREFIX -> str.startsWith(pattern);
            case PATTERN_SUFFIX -> str.endsWith(pattern);
            default -> str.matches(pattern.replace("*", ".*"));
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pattern);
        dest.writeInt(type);
    }

    public static final Creator<PatternMatcher> CREATOR = new Creator<>() {
        @Override
        public PatternMatcher createFromParcel(Parcel source) {
            return new PatternMatcher(source);
        }

        @Override
        public PatternMatcher[] newArray(int size) {
            return new PatternMatcher[size];
        }
    };
}
