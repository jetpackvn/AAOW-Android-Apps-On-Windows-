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
    public static final String BOOTLOADER = "AAOW-BOOT-1.0";
    @Deprecated
    public static final String SERIAL = "AAOWWIN20260001";
    @Deprecated
    public static final String RADIO = "1.0.0.0";
    public static final String HOST = "aaow-windows-host";
    public static final String USER = "aaow";
    public static final long TIME = 1763424000000L;
    public static final String SOC_MANUFACTURER = "AAOW";
    public static final String SOC_MODEL = System.getProperty("os.arch", "amd64");
    public static final String SKU = "windows";
    public static final String ODM_SKU = "windows";
    public static final String TYPE = "user";
    public static final String TAGS = "release-keys";
    public static final String FINGERPRINT = "AAOW/aaow_windows/windows_pc:15/AAOW.261117.001:user/release-keys";
    @Deprecated
    public static final String CPU_ABI = "x86_64";
    @Deprecated
    public static final String CPU_ABI2 = "arm64-v8a";
    public static final String[] SUPPORTED_ABIS = {"x86_64", "arm64-v8a"};
    public static final String[] SUPPORTED_32_BIT_ABIS = {"x86", "armeabi-v7a"};
    public static final String[] SUPPORTED_64_BIT_ABIS = {"x86_64", "arm64-v8a"};

    public static String getRadioVersion() {
        return RADIO;
    }

    public static String getSerial() {
        return SERIAL;
    }

    public static class VERSION {
        public static final String INCREMENTAL = "1";
        public static final String RELEASE = "15";
        public static final String RELEASE_OR_CODENAME = "15";
        public static final String RELEASE_OR_PREVIEW_DISPLAY = "15";
        public static final String BASE_OS = "Windows";
        public static final String SECURITY_PATCH = "2026-11-01";
        public static final int MEDIA_PERFORMANCE_CLASS = 0;
        public static final String SDK = "35";
        public static final int SDK_INT = 35;
        public static final int PREVIEW_SDK_INT = 0;
        public static final String CODENAME = "REL";
    }

    public static class VERSION_CODES {
        public static final int CUR_DEVELOPMENT = 10000;
        public static final int BASE = 1;
        public static final int BASE_1_1 = 2;
        public static final int CUPCAKE = 3;
        public static final int DONUT = 4;
        public static final int ECLAIR = 5;
        public static final int ECLAIR_0_1 = 6;
        public static final int ECLAIR_MR1 = 7;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
        public static final int KITKAT = 19;
        public static final int KITKAT_WATCH = 20;
        public static final int LOLLIPOP = 21;
        public static final int LOLLIPOP_MR1 = 22;
        public static final int M = 23;
        public static final int N = 24;
        public static final int N_MR1 = 25;
        public static final int O = 26;
        public static final int O_MR1 = 27;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
        public static final int S_V2 = 32;
        public static final int TIRAMISU = 33;
        public static final int UPSIDE_DOWN_CAKE = 34;
        public static final int VANILLA_ICE_CREAM = 35;
    }
}
