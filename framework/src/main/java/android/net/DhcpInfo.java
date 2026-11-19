package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/** Thông tin cấu hình DHCP của kết nối mạng hiện tại. */
public class DhcpInfo implements Parcelable {
    public int ipAddress;
    public int gateway;
    public int netmask;
    public int dns1;
    public int dns2;
    public int serverAddress;
    public int leaseDuration;

    public DhcpInfo() {
    }

    public DhcpInfo(DhcpInfo source) {
        if (source != null) {
            this.ipAddress = source.ipAddress;
            this.gateway = source.gateway;
            this.netmask = source.netmask;
            this.dns1 = source.dns1;
            this.dns2 = source.dns2;
            this.serverAddress = source.serverAddress;
            this.leaseDuration = source.leaseDuration;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ipAddress);
        dest.writeInt(gateway);
        dest.writeInt(netmask);
        dest.writeInt(dns1);
        dest.writeInt(dns2);
        dest.writeInt(serverAddress);
        dest.writeInt(leaseDuration);
    }

    public static final Creator<DhcpInfo> CREATOR = new Creator<>() {
        @Override
        public DhcpInfo createFromParcel(Parcel in) {
            DhcpInfo info = new DhcpInfo();
            info.ipAddress = in.readInt();
            info.gateway = in.readInt();
            info.netmask = in.readInt();
            info.dns1 = in.readInt();
            info.dns2 = in.readInt();
            info.serverAddress = in.readInt();
            info.leaseDuration = in.readInt();
            return info;
        }

        @Override
        public DhcpInfo[] newArray(int size) {
            return new DhcpInfo[size];
        }
    };

    @Override
    public String toString() {
        return "ipaddr " + intToIp(ipAddress)
                + " gateway " + intToIp(gateway)
                + " netmask " + intToIp(netmask)
                + " dns1 " + intToIp(dns1)
                + " dns2 " + intToIp(dns2)
                + " DHCP server " + intToIp(serverAddress)
                + " lease " + leaseDuration + " seconds";
    }

    private static String intToIp(int addr) {
        return (addr & 0xFF) + "."
                + ((addr >>> 8) & 0xFF) + "."
                + ((addr >>> 16) & 0xFF) + "."
                + ((addr >>> 24) & 0xFF);
    }
}
