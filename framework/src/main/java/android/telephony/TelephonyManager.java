package android.telephony;

import java.util.Locale;

/**
 * Lớp tương thích {@code android.telephony.TelephonyManager} trên Windows
 * (tránh NullPointerException trong Google DataTransport / Firebase / SDK phân tích mạng).
 */
public class TelephonyManager {
    public static final int PHONE_TYPE_NONE = 0;
    public static final int PHONE_TYPE_GSM = 1;
    public static final int PHONE_TYPE_CDMA = 2;
    public static final int PHONE_TYPE_SIP = 3;

    public static final int SIM_STATE_UNKNOWN = 0;
    public static final int SIM_STATE_ABSENT = 1;
    public static final int SIM_STATE_READY = 5;

    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NETWORK_TYPE_LTE = 13;
    public static final int NETWORK_TYPE_NR = 20;

    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_RINGING = 1;
    public static final int CALL_STATE_OFFHOOK = 2;

    public TelephonyManager() {
    }

    public int getPhoneType() {
        return PHONE_TYPE_NONE;
    }

    public int getSimState() {
        return SIM_STATE_ABSENT;
    }

    public int getSimState(int slotIndex) {
        return SIM_STATE_ABSENT;
    }

    public String getSimOperator() {
        return "";
    }

    public String getSimOperatorName() {
        return "";
    }

    public String getSimCountryIso() {
        String c = Locale.getDefault().getCountry();
        return c != null ? c.toLowerCase(Locale.ROOT) : "us";
    }

    public String getNetworkOperator() {
        return "";
    }

    public String getNetworkOperatorName() {
        return "Wi-Fi";
    }

    public String getNetworkCountryIso() {
        return getSimCountryIso();
    }

    public int getNetworkType() {
        return NETWORK_TYPE_UNKNOWN;
    }

    public int getDataNetworkType() {
        return NETWORK_TYPE_UNKNOWN;
    }

    public int getCallState() {
        return CALL_STATE_IDLE;
    }

    public boolean isNetworkRoaming() {
        return false;
    }

    public String getDeviceId() {
        return "";
    }

    public String getImei() {
        return "";
    }

    public String getSubscriberId() {
        return "";
    }

    public String getLine1Number() {
        return "";
    }

    public int getPhoneCount() {
        return 0;
    }
}
