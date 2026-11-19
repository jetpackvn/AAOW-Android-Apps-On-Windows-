package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Khả năng và loại kết nối của một Network (INTERNET, VALIDATED, NOT_METERED, WIFI/ETHERNET...). */
public final class NetworkCapabilities implements Parcelable {
    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_XCAP = 9;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_NOT_VPN = 15;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
    public static final int NET_CAPABILITY_NOT_ROAMING = 18;
    public static final int NET_CAPABILITY_FOREGROUND = 19;
    public static final int NET_CAPABILITY_NOT_CONGESTED = 20;
    public static final int NET_CAPABILITY_NOT_SUSPENDED = 21;
    public static final int NET_CAPABILITY_MCX = 23;
    public static final int NET_CAPABILITY_TEMPORARILY_NOT_METERED = 25;
    public static final int NET_CAPABILITY_HEAD_UNIT = 32;
    public static final int NET_CAPABILITY_MMTEL = 33;
    public static final int NET_CAPABILITY_PRIORITIZE_LATENCY = 34;
    public static final int NET_CAPABILITY_PRIORITIZE_BANDWIDTH = 35;
    public static final int NET_CAPABILITY_LOCAL_NETWORK = 36;
    public static final int NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED = 37;
    public static final int NET_CAPABILITY_PRIORITIZE_UNIFIED_COMMUNICATIONS = 38;

    public static final int NET_ENTERPRISE_ID_1 = 1;
    public static final int NET_ENTERPRISE_ID_2 = 2;
    public static final int NET_ENTERPRISE_ID_3 = 3;
    public static final int NET_ENTERPRISE_ID_4 = 4;
    public static final int NET_ENTERPRISE_ID_5 = 5;

    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI_AWARE = 5;
    public static final int TRANSPORT_LOWPAN = 6;
    public static final int TRANSPORT_USB = 8;
    public static final int TRANSPORT_THREAD = 9;
    public static final int TRANSPORT_SATELLITE = 10;

    public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;

    private final Set<Integer> capabilities = new LinkedHashSet<>();
    private final Set<Integer> transports = new LinkedHashSet<>();
    private final Set<Integer> enterpriseIds = new LinkedHashSet<>();
    private int linkDownBandwidthKbps = 100_000;
    private int linkUpBandwidthKbps = 50_000;
    private int signalStrength = -50;
    private int ownerUid = -1;
    private String ssid = "\"AAOW-Windows-WiFi\"";
    private TransportInfo transportInfo;

    public NetworkCapabilities() {
    }

    public NetworkCapabilities(NetworkCapabilities nc) {
        if (nc != null) {
            this.capabilities.addAll(nc.capabilities);
            this.transports.addAll(nc.transports);
            this.enterpriseIds.addAll(nc.enterpriseIds);
            this.linkDownBandwidthKbps = nc.linkDownBandwidthKbps;
            this.linkUpBandwidthKbps = nc.linkUpBandwidthKbps;
            this.signalStrength = nc.signalStrength;
            this.ownerUid = nc.ownerUid;
            this.ssid = nc.ssid;
            this.transportInfo = nc.transportInfo;
        }
    }

    public NetworkCapabilities addCapability(int capability) {
        capabilities.add(capability);
        return this;
    }

    public NetworkCapabilities removeCapability(int capability) {
        capabilities.remove(capability);
        return this;
    }

    public NetworkCapabilities addTransportType(int transportType) {
        transports.add(transportType);
        return this;
    }

    public NetworkCapabilities removeTransportType(int transportType) {
        transports.remove(transportType);
        return this;
    }

    public boolean hasCapability(int capability) {
        return capabilities.contains(capability);
    }

    public boolean hasTransport(int transportType) {
        return transports.contains(transportType);
    }

    public boolean hasEnterpriseId(int enterpriseId) {
        return enterpriseIds.contains(enterpriseId);
    }

    public int[] getCapabilities() {
        return capabilities.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getTransportTypes() {
        return transports.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getEnterpriseIds() {
        return enterpriseIds.stream().mapToInt(Integer::intValue).toArray();
    }

    public Set<Integer> getSubscriptionIds() {
        return Collections.emptySet();
    }

    public int getLinkDownstreamBandwidthKbps() {
        return linkDownBandwidthKbps;
    }

    public int getLinkUpstreamBandwidthKbps() {
        return linkUpBandwidthKbps;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public int getOwnerUid() {
        return ownerUid;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public TransportInfo getTransportInfo() {
        return transportInfo;
    }

    public void setTransportInfo(TransportInfo transportInfo) {
        this.transportInfo = transportInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(getCapabilities());
        dest.writeIntArray(getTransportTypes());
        dest.writeInt(linkDownBandwidthKbps);
        dest.writeInt(linkUpBandwidthKbps);
        dest.writeInt(signalStrength);
    }

    public static final Creator<NetworkCapabilities> CREATOR = new Creator<>() {
        @Override
        public NetworkCapabilities createFromParcel(Parcel in) {
            NetworkCapabilities nc = new NetworkCapabilities();
            for (int c : in.createIntArray()) nc.addCapability(c);
            for (int t : in.createIntArray()) nc.addTransportType(t);
            nc.linkDownBandwidthKbps = in.readInt();
            nc.linkUpBandwidthKbps = in.readInt();
            nc.signalStrength = in.readInt();
            return nc;
        }

        @Override
        public NetworkCapabilities[] newArray(int size) {
            return new NetworkCapabilities[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof NetworkCapabilities other
                && Objects.equals(capabilities, other.capabilities)
                && Objects.equals(transports, other.transports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilities, transports);
    }

    @Override
    public String toString() {
        return "[ Transports: " + transports + " Capabilities: " + capabilities + "]";
    }
}
