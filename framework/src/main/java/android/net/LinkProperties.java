package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thuộc tính liên kết mạng (địa chỉ IP, DNS servers, Routes, MTU, Proxy) của một Network trên Windows.
 */
public final class LinkProperties implements Parcelable {
    private String ifaceName;
    private final List<LinkAddress> linkAddresses = new ArrayList<>();
    private final List<InetAddress> dnsServers = new ArrayList<>();
    private final List<RouteInfo> routes = new ArrayList<>();
    private String domains;
    private int mtu = 1500;
    private ProxyInfo httpProxy;
    private Inet4Address dhcpServerAddress;
    private IpPrefix nat64Prefix;
    private boolean privateDnsActive;
    private String privateDnsServerName;

    public LinkProperties() {
    }

    public LinkProperties(LinkProperties source) {
        if (source != null) {
            this.ifaceName = source.ifaceName;
            this.linkAddresses.addAll(source.linkAddresses);
            this.dnsServers.addAll(source.dnsServers);
            this.routes.addAll(source.routes);
            this.domains = source.domains;
            this.mtu = source.mtu;
            this.httpProxy = source.httpProxy != null ? new ProxyInfo(source.httpProxy) : null;
            this.dhcpServerAddress = source.dhcpServerAddress;
            this.nat64Prefix = source.nat64Prefix;
            this.privateDnsActive = source.privateDnsActive;
            this.privateDnsServerName = source.privateDnsServerName;
        }
    }

    public String getInterfaceName() {
        return ifaceName;
    }

    public void setInterfaceName(String iface) {
        this.ifaceName = iface;
    }

    public List<LinkAddress> getLinkAddresses() {
        return Collections.unmodifiableList(new ArrayList<>(linkAddresses));
    }

    public void setLinkAddresses(Collection<LinkAddress> addresses) {
        linkAddresses.clear();
        if (addresses != null) {
            for (LinkAddress la : addresses) {
                if (la != null) linkAddresses.add(la);
            }
        }
    }

    public boolean addLinkAddress(LinkAddress address) {
        if (address != null && !linkAddresses.contains(address)) {
            linkAddresses.add(address);
            return true;
        }
        return false;
    }

    public List<InetAddress> getAddresses() {
        List<InetAddress> result = new ArrayList<>(linkAddresses.size());
        for (LinkAddress la : linkAddresses) {
            if (la.getAddress() != null) result.add(la.getAddress());
        }
        return result;
    }

    public List<InetAddress> getDnsServers() {
        return Collections.unmodifiableList(new ArrayList<>(dnsServers));
    }

    public void setDnsServers(Collection<InetAddress> dnsServers) {
        this.dnsServers.clear();
        if (dnsServers != null) {
            for (InetAddress dns : dnsServers) {
                if (dns != null) this.dnsServers.add(dns);
            }
        }
    }

    public boolean addDnsServer(InetAddress dnsServer) {
        if (dnsServer != null && !dnsServers.contains(dnsServer)) {
            dnsServers.add(dnsServer);
            return true;
        }
        return false;
    }

    public List<RouteInfo> getRoutes() {
        return Collections.unmodifiableList(new ArrayList<>(routes));
    }

    public boolean addRoute(RouteInfo route) {
        if (route != null && !routes.contains(route)) {
            routes.add(route);
            return true;
        }
        return false;
    }

    public String getDomains() {
        return domains;
    }

    public void setDomains(String domains) {
        this.domains = domains;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public ProxyInfo getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(ProxyInfo proxy) {
        this.httpProxy = proxy;
    }

    public Inet4Address getDhcpServerAddress() {
        return dhcpServerAddress;
    }

    public void setDhcpServerAddress(Inet4Address serverAddress) {
        this.dhcpServerAddress = serverAddress;
    }

    public IpPrefix getNat64Prefix() {
        return nat64Prefix;
    }

    public void setNat64Prefix(IpPrefix prefix) {
        this.nat64Prefix = prefix;
    }

    public boolean isPrivateDnsActive() {
        return privateDnsActive;
    }

    public String getPrivateDnsServerName() {
        return privateDnsServerName;
    }

    public boolean isWakeOnLanSupported() {
        return false;
    }

    public void clear() {
        ifaceName = null;
        linkAddresses.clear();
        dnsServers.clear();
        routes.clear();
        domains = null;
        mtu = 0;
        httpProxy = null;
        dhcpServerAddress = null;
        nat64Prefix = null;
        privateDnsActive = false;
        privateDnsServerName = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ifaceName);
        dest.writeInt(mtu);
        dest.writeString(domains);
        dest.writeInt(linkAddresses.size());
        for (LinkAddress la : linkAddresses) {
            la.writeToParcel(dest, flags);
        }
        dest.writeInt(dnsServers.size());
        for (InetAddress dns : dnsServers) {
            dest.writeByteArray(dns.getAddress());
        }
        dest.writeInt(routes.size());
        for (RouteInfo ri : routes) {
            ri.writeToParcel(dest, flags);
        }
    }

    public static final Creator<LinkProperties> CREATOR = new Creator<>() {
        @Override
        public LinkProperties createFromParcel(Parcel in) {
            LinkProperties lp = new LinkProperties();
            lp.setInterfaceName(in.readString());
            lp.setMtu(in.readInt());
            lp.setDomains(in.readString());
            int laCount = in.readInt();
            for (int i = 0; i < laCount; i++) {
                lp.addLinkAddress(LinkAddress.CREATOR.createFromParcel(in));
            }
            int dnsCount = in.readInt();
            for (int i = 0; i < dnsCount; i++) {
                byte[] b = in.createByteArray();
                if (b != null) {
                    try {
                        lp.addDnsServer(InetAddress.getByAddress(b));
                    } catch (Exception ignored) {
                    }
                }
            }
            int routeCount = in.readInt();
            for (int i = 0; i < routeCount; i++) {
                lp.addRoute(RouteInfo.CREATOR.createFromParcel(in));
            }
            return lp;
        }

        @Override
        public LinkProperties[] newArray(int size) {
            return new LinkProperties[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkProperties other
                && mtu == other.mtu
                && Objects.equals(ifaceName, other.ifaceName)
                && Objects.equals(linkAddresses, other.linkAddresses)
                && Objects.equals(dnsServers, other.dnsServers)
                && Objects.equals(routes, other.routes)
                && Objects.equals(domains, other.domains)
                && Objects.equals(httpProxy, other.httpProxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ifaceName, linkAddresses, dnsServers, routes, domains, mtu, httpProxy);
    }

    @Override
    public String toString() {
        return "{InterfaceName: " + ifaceName
                + " LinkAddresses: " + linkAddresses
                + " DnsAddresses: " + dnsServers
                + " Routes: " + routes
                + " MTU: " + mtu + "}";
    }
}
