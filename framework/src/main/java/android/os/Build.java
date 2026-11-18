package android.os;

/** Giả lập thông tin thiết bị Android trên Windows. */
public class Build {
    public static final String UNKNOWN = "unknown";
    public static final String ID = "AAOW.261117.001";
    public static final String DISPLAY = "AAOW Windows Desktop Layer 1.0";
    public static final String PRODUCT = "aaow_windows_x86_64";
    public static final String DEVICE = "windows_pc";
    public static final String BOARD = "windows";
    public static final String MANUFACTURER = "AAOW Project";
    public static final String BRAND = "AAOW";
    public static final String MODEL = "Android Apps On Windows (" + System.getProperty("os.name") + ")";
    public static final String HARDWARE = System.getProperty("os.arch", "amd64");
    public static final String TYPE = "user";
    public static final String TAGS = "release-keys";
    public static final String FINGERPRINT = "AAOW/aaow_windows/windows_pc:15/AAOW.261117.001:user/release-keys";
    public static final String[] SUPPORTED_ABIS = {"x86_64", "arm64-v8a"};
    public static final String[] SUPPORTED_32_BIT_ABIS = {"x86", "armeabi-v7a"};
    public static final String[] SUPPORTED_64_BIT_ABIS = {"x86_64", "arm64-v8a"};

    public static class VERSION {
        public static final String INCREMENTAL = "1";
        public static final String RELEASE = "15";
        public static final String BASE_OS = "Windows";
        public static final String SDK = "35";
        public static final int SDK_INT = 35;
        public static final int PREVIEW_SDK_INT = 0;
        public static final String CODENAME = "REL";
    }

    public static class VERSION_CODES {
        public static final int BASE = 1;
        public static final int ECLAIR = 5;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int HONEYCOMB = 11;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int JELLY_BEAN = 16;
        public static final int KITKAT = 19;
        public static final int LOLLIPOP = 21;
        public static final int M = 23;
        public static final int N = 24;
        public static final int O = 26;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
        public static final int TIRAMISU = 33;
        public static final int UPSIDE_DOWN_CAKE = 34;
        public static final int VANILLA_ICE_CREAM = 35;
    }
}
