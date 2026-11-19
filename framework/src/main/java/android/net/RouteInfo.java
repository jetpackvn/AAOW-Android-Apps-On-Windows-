package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/** Thông tin định tuyến mạng (RouteInfo) dùng trong LinkProperties. */
public final class RouteInfo implements Parcelable {
    public static final int RTN_UNICAST = 1;
    public static final int RTN_UNREACHABLE = 7;
    public static final int RTN_THROW = 9;

    private final IpPrefix destination;
    private final InetAddress gateway;
    private final String iface;
    private final int type;

    public RouteInfo(IpPrefix destination, InetAddress gateway, String iface, int type) {
        if (destination == null) {
            try {
                destination = new IpPrefix(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 0);
            } catch (UnknownHostException ignored) {
            }
        }
        this.destination = destination;
        this.gateway = gateway;
        this.iface = iface;
        this.type = type;
    }

    public RouteInfo(IpPrefix destination, InetAddress gateway, String iface) {
        this(destination, gateway, iface, RTN_UNICAST);
    }

    public IpPrefix getDestination() {
        return destination;
    }

    public InetAddress getGateway() {
        return gateway;
    }

    public String getInterface() {
        return iface;
    }

    public int getType() {
        return type;
    }

    public boolean isDefaultRoute() {
        return type == RTN_UNICAST && destination != null && destination.getPrefixLength() == 0;
    }

    public boolean hasGateway() {
        return gateway != null && !gateway.isAnyLocalAddress();
    }

    public boolean matches(InetAddress dest) {
        return destination != null && destination.contains(dest);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (destination != null) {
            dest.writeInt(1);
            destination.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeByteArray(gateway != null ? gateway.getAddress() : null);
        dest.writeString(iface);
        dest.writeInt(type);
    }

    public static final Creator<RouteInfo> CREATOR = new Creator<>() {
        @Override
        public RouteInfo createFromParcel(Parcel in) {
            IpPrefix prefix = in.readInt() != 0 ? IpPrefix.CREATOR.createFromParcel(in) : null;
            byte[] gwBytes = in.createByteArray();
            InetAddress gw = null;
            if (gwBytes != null && gwBytes.length > 0) {
                try {
                    gw = InetAddress.getByAddress(gwBytes);
                } catch (UnknownHostException ignored) {
                }
            }
            String ifName = in.readString();
            int t = in.readInt();
            return new RouteInfo(prefix, gw, ifName, t);
        }

        @Override
        public RouteInfo[] newArray(int size) {
            return new RouteInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof RouteInfo other
                && type == other.type
                && Objects.equals(destination, other.destination)
                && Objects.equals(gateway, other.gateway)
                && Objects.equals(iface, other.iface);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, gateway, iface, type);
    }

    @Override
    public String toString() {
        return (destination != null ? destination.toString() : "default")
                + (gateway != null ? " -> " + gateway.getHostAddress() : "")
                + (iface != null ? " dev " + iface : "");
    }
}
