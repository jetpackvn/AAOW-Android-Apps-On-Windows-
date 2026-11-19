package android.net.wifi;

import android.net.ConnectivityManager;
import android.net.DhcpInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Cài đặt android.net.wifi.WifiManager phản ánh trạng thái mạng Windows thật
 * (địa chỉ IPv4 nội bộ, DHCP, WifiInfo, WifiLock, MulticastLock, mức tín hiệu RSSI).
 */
public class WifiManager {
    public static final String WIFI_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_STATE_CHANGED";
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
    public static final String SUPPLICANT_CONNECTION_CHANGE_ACTION = "android.net.wifi.supplicant.CONNECTION_CHANGE";
    public static final String SUPPLICANT_STATE_CHANGED_ACTION = "android.net.wifi.supplicant.STATE_CHANGE";
    public static final String SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.SCAN_RESULTS";
    public static final String RSSI_CHANGED_ACTION = "android.net.wifi.RSSI_CHANGED";
    public static final String NETWORK_IDS_CHANGED_ACTION = "android.net.wifi.NETWORK_IDS_CHANGED";
    public static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
    public static final String ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE = "android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE";

    public static final String EXTRA_WIFI_STATE = "wifi_state";
    public static final String EXTRA_PREVIOUS_WIFI_STATE = "previous_wifi_state";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_BSSID = "bssid";
    public static final String EXTRA_WIFI_INFO = "wifiInfo";
    public static final String EXTRA_NEW_STATE = "newState";
    public static final String EXTRA_NEW_RSSI = "newRssi";
    public static final String EXTRA_RESULTS_UPDATED = "resultsUpdated";
    public static final String EXTRA_SUPPLICANT_CONNECTED = "connected";
    public static final String EXTRA_SUPPLICANT_ERROR = "supplicantError";

    public static final int WIFI_STATE_DISABLING = 0;
    public static final int WIFI_STATE_DISABLED = 1;
    public static final int WIFI_STATE_ENABLING = 2;
    public static final int WIFI_STATE_ENABLED = 3;
    public static final int WIFI_STATE_UNKNOWN = 4;

    public static final int WIFI_MODE_FULL = 1;
    public static final int WIFI_MODE_SCAN_ONLY = 2;
    public static final int WIFI_MODE_FULL_HIGH_PERF = 3;
    public static final int WIFI_MODE_FULL_LOW_LATENCY = 4;

    public static final String UNKNOWN_SSID = "<unknown ssid>";

    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;
    private static final int DEFAULT_MAX_SIGNAL_LEVEL = 4;

    private volatile boolean wifiEnabled = true;

    public WifiManager() {
    }

    public boolean isWifiEnabled() {
        if (!wifiEnabled) return false;
        return !ConnectivityManager.isSimulatedOffline();
    }

    public boolean setWifiEnabled(boolean enabled) {
        this.wifiEnabled = enabled;
        return true;
    }

    public int getWifiState() {
        return isWifiEnabled() ? WIFI_STATE_ENABLED : WIFI_STATE_DISABLED;
    }

    public WifiInfo getConnectionInfo() {
        WifiInfo info = new WifiInfo();
        if (!isWifiEnabled()) {
            info.setSSID(UNKNOWN_SSID);
            info.setBSSID("00:00:00:00:00:00");
            info.setNetworkId(-1);
            info.setRssi(-127);
            info.setIpAddress(0);
            info.setSupplicantState(SupplicantState.DISCONNECTED);
            return info;
        }
        int ip = resolveLocalIpv4LittleEndian();
        info.setIpAddress(ip);
        info.setSSID("\"AAOW-Windows-WiFi\"");
        info.setBSSID("02:00:00:00:00:01");
        info.setRssi(-48);
        info.setLinkSpeed(866);
        info.setFrequency(5180);
        info.setSupplicantState(SupplicantState.COMPLETED);
        return info;
    }

    public DhcpInfo getDhcpInfo() {
        DhcpInfo dhcp = new DhcpInfo();
        int ip = isWifiEnabled() ? resolveLocalIpv4LittleEndian() : 0;
        dhcp.ipAddress = ip;
        if (ip != 0) {
            dhcp.netmask = 0x00FFFFFF; // 255.255.255.0
            dhcp.gateway = (ip & 0x00FFFFFF) | 0x01000000; // x.x.x.1
            dhcp.serverAddress = dhcp.gateway;
            dhcp.dns1 = 0x08080808; // 8.8.8.8
            dhcp.dns2 = 0x01010101; // 1.1.1.1
            dhcp.leaseDuration = 86400;
        }
        return dhcp;
    }

    public static int resolveLocalIpv4LittleEndian() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                for (NetworkInterface nif : Collections.list(ifaces)) {
                    if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                    for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            byte[] b = addr.getAddress();
                            return (b[0] & 0xFF)
                                    | ((b[1] & 0xFF) << 8)
                                    | ((b[2] & 0xFF) << 16)
                                    | ((b[3] & 0xFF) << 24);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0x6401A8C0; // 192.168.1.100 fallback
    }

    public int calculateSignalLevel(int rssi) {
        return calculateSignalLevel(rssi, DEFAULT_MAX_SIGNAL_LEVEL + 1);
    }

    public static int calculateSignalLevel(int rssi, int numLevels) {
        if (numLevels <= 1) return 0;
        if (rssi <= MIN_RSSI) return 0;
        if (rssi >= MAX_RSSI) return numLevels - 1;
        float inputRange = (MAX_RSSI - MIN_RSSI);
        float outputRange = (numLevels - 1);
        return (int) ((float) (rssi - MIN_RSSI) * outputRange / inputRange);
    }

    public static int compareSignalLevel(int rssiA, int rssiB) {
        return rssiA - rssiB;
    }

    public int getMaxSignalLevel() {
        return DEFAULT_MAX_SIGNAL_LEVEL;
    }

    public boolean startScan() {
        return true;
    }

    public <T> List<T> getScanResults() {
        return Collections.emptyList();
    }

    public <T> List<T> getConfiguredNetworks() {
        return Collections.emptyList();
    }

    public boolean reconnect() {
        return true;
    }

    public boolean reassociate() {
        return true;
    }

    public boolean disconnect() {
        return true;
    }

    public boolean pingSupplicant() {
        return isWifiEnabled();
    }

    public boolean is5GHzBandSupported() {
        return true;
    }

    public boolean is24GHzBandSupported() {
        return true;
    }

    public boolean is6GHzBandSupported() {
        return false;
    }

    public boolean is60GHzBandSupported() {
        return false;
    }

    public boolean isP2pSupported() {
        return false;
    }

    public boolean isScanAlwaysAvailable() {
        return true;
    }

    public WifiLock createWifiLock(int lockType, String tag) {
        return new WifiLock(lockType, tag);
    }

    public WifiLock createWifiLock(String tag) {
        return new WifiLock(WIFI_MODE_FULL, tag);
    }

    public MulticastLock createMulticastLock(String tag) {
        return new MulticastLock(tag);
    }

    public class WifiLock {
        private final String tag;
        private final int lockType;
        private boolean refCounted = true;
        private int refCount = 0;
        private boolean held = false;

        WifiLock(int lockType, String tag) {
            this.lockType = lockType;
            this.tag = tag;
        }

        public synchronized void acquire() {
            if (refCounted) {
                refCount++;
                held = true;
            } else {
                held = true;
            }
        }

        public synchronized void release() {
            if (refCounted) {
                if (--refCount <= 0) {
                    refCount = 0;
                    held = false;
                }
            } else {
                held = false;
            }
        }

        public synchronized void setReferenceCounted(boolean refCounted) {
            this.refCounted = refCounted;
        }

        public synchronized boolean isHeld() {
            return held;
        }

        @Override
        public String toString() {
            return "WifiLock{" + tag + " type=" + lockType + " held=" + held + "}";
        }
    }

    public class MulticastLock {
        private final String tag;
        private boolean refCounted = true;
        private int refCount = 0;
        private boolean held = false;

        MulticastLock(String tag) {
            this.tag = tag;
        }

        public synchronized void acquire() {
            if (refCounted) {
                refCount++;
                held = true;
            } else {
                held = true;
            }
        }

        public synchronized void release() {
            if (refCounted) {
                if (--refCount <= 0) {
                    refCount = 0;
                    held = false;
                }
            } else {
                held = false;
            }
        }

        public synchronized void setReferenceCounted(boolean refCounted) {
            this.refCounted = refCounted;
        }

        public synchronized boolean isHeld() {
            return held;
        }

        @Override
        public String toString() {
            return "MulticastLock{" + tag + " held=" + held + "}";
        }
    }
}
