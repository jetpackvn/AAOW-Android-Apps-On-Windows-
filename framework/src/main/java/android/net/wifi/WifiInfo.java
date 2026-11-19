package android.net.wifi;

import android.net.NetworkInfo;
import android.net.TransportInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Thông tin kết nối Wi-Fi hiện tại (SSID, BSSID, địa chỉ IP, tốc độ liên kết, RSSI)
 * tương thích với android.net.wifi.WifiInfo và NetworkCapabilities.getTransportInfo().
 */
public class WifiInfo implements Parcelable, TransportInfo {
    public static final String LINK_SPEED_UNITS = "Mbps";
    public static final String FREQUENCY_UNITS = "MHz";
    public static final int LINK_SPEED_UNKNOWN = -1;

    public static final int SECURITY_TYPE_UNKNOWN = -1;
    public static final int SECURITY_TYPE_OPEN = 0;
    public static final int SECURITY_TYPE_WEP = 1;
    public static final int SECURITY_TYPE_PSK = 2;
    public static final int SECURITY_TYPE_EAP = 3;
    public static final int SECURITY_TYPE_SAE = 4;
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT = 5;
    public static final int SECURITY_TYPE_OWE = 6;
    public static final int SECURITY_TYPE_WAPI_PSK = 7;
    public static final int SECURITY_TYPE_WAPI_CERT = 8;
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE = 9;
    public static final int SECURITY_TYPE_OSEN = 10;
    public static final int SECURITY_TYPE_PASSPOINT_R1_R2 = 11;
    public static final int SECURITY_TYPE_PASSPOINT_R3 = 12;
    public static final int SECURITY_TYPE_DPP = 13;

    private String ssid = "\"AAOW-Windows-WiFi\"";
    private String bssid = "02:00:00:00:00:01";
    private String macAddress = "02:00:00:00:00:00";
    private int networkId = 1;
    private int rssi = -50;
    private int linkSpeed = 866;
    private int txLinkSpeedMbps = 866;
    private int rxLinkSpeedMbps = 866;
    private int frequency = 5180;
    private int ipAddress = 0x0101A8C0; // 192.168.1.1 (little-endian int convention)
    private boolean hiddenSsid = false;
    private SupplicantState supplicantState = SupplicantState.COMPLETED;
    private int securityType = SECURITY_TYPE_SAE;

    public WifiInfo() {
    }

    public WifiInfo(String ssid, String bssid, int ipAddress, int rssi, int linkSpeedMbps) {
        if (ssid != null) {
            this.ssid = ssid.startsWith("\"") ? ssid : "\"" + ssid + "\"";
        }
        if (bssid != null) this.bssid = bssid;
        this.ipAddress = ipAddress;
        this.rssi = rssi;
        this.linkSpeed = linkSpeedMbps;
        this.txLinkSpeedMbps = linkSpeedMbps;
        this.rxLinkSpeedMbps = linkSpeedMbps;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        if (ssid == null) {
            this.ssid = WifiManager.UNKNOWN_SSID;
        } else {
            this.ssid = ssid.startsWith("\"") ? ssid : "\"" + ssid + "\"";
        }
    }

    public String getBSSID() {
        return bssid;
    }

    public void setBSSID(String bssid) {
        this.bssid = bssid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getLinkSpeed() {
        return linkSpeed;
    }

    public void setLinkSpeed(int linkSpeed) {
        this.linkSpeed = linkSpeed;
    }

    public int getTxLinkSpeedMbps() {
        return txLinkSpeedMbps;
    }

    public int getRxLinkSpeedMbps() {
        return rxLinkSpeedMbps;
    }

    public int getMaxSupportedTxLinkSpeedMbps() {
        return 1200;
    }

    public int getMaxSupportedRxLinkSpeedMbps() {
        return 1200;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getNetId() {
        return networkId;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        if (macAddress != null) this.macAddress = macAddress;
    }

    public int getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(int ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean getHiddenSSID() {
        return hiddenSsid;
    }

    public SupplicantState getSupplicantState() {
        return supplicantState;
    }

    public void setSupplicantState(SupplicantState state) {
        this.supplicantState = state != null ? state : SupplicantState.COMPLETED;
    }

    public int getCurrentSecurityType() {
        return securityType;
    }

    public int getWifiStandard() {
        return 6; //ScanResult.WIFI_STANDARD_11AX
    }

    public int getSubscriptionId() {
        return -1;
    }

    public boolean isRestricted() {
        return false;
    }

    public String getPasspointFqdn() {
        return null;
    }

    public String getPasspointProviderFriendlyName() {
        return null;
    }

    public String getPasspointUniqueId() {
        return null;
    }

    public long getApplicableRedactions() {
        return 0L;
    }

    public WifiInfo makeCopy(long redactions) {
        return new WifiInfo(ssid, bssid, ipAddress, rssi, linkSpeed);
    }

    public static NetworkInfo.DetailedState getDetailedStateOf(SupplicantState suppState) {
        if (suppState == null) return NetworkInfo.DetailedState.IDLE;
        return switch (suppState) {
            case DISCONNECTED, INTERFACE_DISABLED -> NetworkInfo.DetailedState.DISCONNECTED;
            case INACTIVE, DORMANT, UNINITIALIZED, INVALID -> NetworkInfo.DetailedState.IDLE;
            case SCANNING -> NetworkInfo.DetailedState.SCANNING;
            case AUTHENTICATING, FOUR_WAY_HANDSHAKE, GROUP_HANDSHAKE -> NetworkInfo.DetailedState.AUTHENTICATING;
            case ASSOCIATING -> NetworkInfo.DetailedState.CONNECTING;
            case ASSOCIATED -> NetworkInfo.DetailedState.OBTAINING_IPADDR;
            case COMPLETED -> NetworkInfo.DetailedState.CONNECTED;
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ssid);
        dest.writeString(bssid);
        dest.writeString(macAddress);
        dest.writeInt(networkId);
        dest.writeInt(rssi);
        dest.writeInt(linkSpeed);
        dest.writeInt(frequency);
        dest.writeInt(ipAddress);
        dest.writeString(supplicantState.name());
    }

    public static final Creator<WifiInfo> CREATOR = new Creator<>() {
        @Override
        public WifiInfo createFromParcel(Parcel in) {
            WifiInfo info = new WifiInfo();
            info.ssid = in.readString();
            info.bssid = in.readString();
            info.macAddress = in.readString();
            info.networkId = in.readInt();
            info.rssi = in.readInt();
            info.linkSpeed = in.readInt();
            info.frequency = in.readInt();
            info.ipAddress = in.readInt();
            String st = in.readString();
            if (st != null) {
                try {
                    info.supplicantState = SupplicantState.valueOf(st);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return info;
        }

        @Override
        public WifiInfo[] newArray(int size) {
            return new WifiInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof WifiInfo other
                && networkId == other.networkId
                && rssi == other.rssi
                && linkSpeed == other.linkSpeed
                && frequency == other.frequency
                && ipAddress == other.ipAddress
                && Objects.equals(ssid, other.ssid)
                && Objects.equals(bssid, other.bssid)
                && supplicantState == other.supplicantState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssid, bssid, networkId, rssi, linkSpeed, frequency, ipAddress, supplicantState);
    }

    @Override
    public String toString() {
        return "SSID: " + ssid + ", BSSID: " + bssid + ", Supplicant state: " + supplicantState
                + ", RSSI: " + rssi + ", Link speed: " + linkSpeed + LINK_SPEED_UNITS
                + ", Frequency: " + frequency + FREQUENCY_UNITS;
    }
}
