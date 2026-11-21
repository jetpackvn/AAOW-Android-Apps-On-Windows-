package android.net;

import android.app.PendingIntent;
import android.net.wifi.WifiInfo;
import android.os.Handler;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Cài đặt android.net.ConnectivityManager phản ánh trạng thái mạng thật của Windows
 * (quét giao diện mạng qua java.net.NetworkInterface để báo Wi-Fi / Ethernet, LinkProperties và trạng thái Online).
 */
public class ConnectivityManager {
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED = "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";
    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";
    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED = "android.net.conn.RESTRICT_BACKGROUND_CHANGED";

    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";
    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";
    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";
    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";

    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE_MMS = 2;
    public static final int TYPE_MOBILE_SUPL = 3;
    public static final int TYPE_MOBILE_DUN = 4;
    public static final int TYPE_MOBILE_HIPRI = 5;
    public static final int TYPE_WIMAX = 6;
    public static final int TYPE_BLUETOOTH = 7;
    public static final int TYPE_DUMMY = 8;
    public static final int TYPE_ETHERNET = 9;
    public static final int TYPE_VPN = 17;

    public static final int DEFAULT_NETWORK_PREFERENCE = TYPE_WIFI;

    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1 << 0;
    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 1 << 1;
    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 1 << 2;

    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

    public static class NetworkCallback {
        public static final int FLAG_INCLUDE_LOCATION_INFO = 1 << 0;

        public NetworkCallback() {
        }

        public NetworkCallback(int flags) {
        }

        public void onAvailable(Network network) {
        }

        public void onLosing(Network network, int maxMsToLive) {
        }

        public void onLost(Network network) {
        }

        public void onUnavailable() {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        public void onBlockedStatusChanged(Network network, boolean blocked) {
        }

        public void onReserved(NetworkCapabilities networkCapabilities) {
        }
    }

    public interface OnNetworkActiveListener {
        void onNetworkActive();
    }

    private record WindowsNetProbe(boolean connected, boolean wifi, String ifaceName,
                                   String sysIfaceName, int mtu, List<LinkAddress> linkAddresses) {
    }

    private record CallbackRegistration(NetworkRequest request, NetworkCallback callback, Handler handler) {
    }

    private static volatile Network processBoundNetwork;
    private static volatile boolean simulatedOffline = false;
    private static volatile Boolean simulatedWifiOverride = null;
    private static final java.util.List<java.lang.ref.WeakReference<ConnectivityManager>> INSTANCES =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private final Map<NetworkCallback, CallbackRegistration> registeredCallbacks = new ConcurrentHashMap<>();
    private final Set<OnNetworkActiveListener> activeListeners = new CopyOnWriteArraySet<>();
    private int networkPreference = DEFAULT_NETWORK_PREFERENCE;

    public ConnectivityManager() {
        INSTANCES.add(new java.lang.ref.WeakReference<>(this));
    }

    public static void setSimulatedOffline(boolean offline) {
        boolean changed = simulatedOffline != offline;
        simulatedOffline = offline;
        if (changed) {
            for (java.lang.ref.WeakReference<ConnectivityManager> ref : INSTANCES) {
                ConnectivityManager cm = ref.get();
                if (cm == null) {
                    INSTANCES.remove(ref);
                } else {
                    cm.notifyNetworkStateChanged();
                }
            }
        }
    }

    public static boolean isSimulatedOffline() {
        return simulatedOffline;
    }

    public static void setSimulatedWifiOverride(Boolean wifiOverride) {
        simulatedWifiOverride = wifiOverride;
    }

    private static WindowsNetProbe probeWindowsNetwork() {
        if (simulatedOffline) {
            return new WindowsNetProbe(false, false, null, null, 1500, Collections.emptyList());
        }
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                boolean anyUpRoutable = false;
                boolean foundWifi = false;
                String bestDisplay = null;
                String bestSysName = null;
                int bestMtu = 1500;
                List<LinkAddress> bestAddrs = new ArrayList<>();

                for (NetworkInterface nif : Collections.list(ifaces)) {
                    if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                    List<LinkAddress> ifaceAddrs = new ArrayList<>();
                    for (InterfaceAddress ia : nif.getInterfaceAddresses()) {
                        InetAddress addr = ia.getAddress();
                        if (addr != null && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            ifaceAddrs.add(new LinkAddress(ia));
                        }
                    }
                    if (ifaceAddrs.isEmpty()) continue;

                    anyUpRoutable = true;
                    String label = ((nif.getName() != null ? nif.getName() : "") + " "
                            + (nif.getDisplayName() != null ? nif.getDisplayName() : "")).toLowerCase(Locale.ROOT);
                    boolean isWifi = label.contains("wi-fi") || label.contains("wifi") || label.contains("wlan")
                            || label.contains("wireless") || label.contains("802.11");
                    if (isWifi) {
                        foundWifi = true;
                        bestDisplay = nif.getDisplayName();
                        bestSysName = nif.getName();
                        bestMtu = nif.getMTU() > 0 ? nif.getMTU() : 1500;
                        bestAddrs = ifaceAddrs;
                        break;
                    }
                    if (bestDisplay == null) {
                        bestDisplay = nif.getDisplayName();
                        bestSysName = nif.getName();
                        bestMtu = nif.getMTU() > 0 ? nif.getMTU() : 1500;
                        bestAddrs = ifaceAddrs;
                    }
                }
                if (anyUpRoutable) {
                    boolean reportWifi = simulatedWifiOverride != null ? simulatedWifiOverride : foundWifi;
                    return new WindowsNetProbe(true, reportWifi, bestDisplay,
                            bestSysName != null ? bestSysName : "wlan0", bestMtu, bestAddrs);
                }
            }
        } catch (Throwable ignored) {
        }
        return new WindowsNetProbe(false, false, null, null, 1500, Collections.emptyList());
    }

    public static boolean isNetworkTypeValid(int networkType) {
        return networkType >= TYPE_MOBILE && networkType <= TYPE_VPN;
    }

    public Network getActiveNetwork() {
        return probeWindowsNetwork().connected() ? new Network(100) : null;
    }

    public Network[] getAllNetworks() {
        Network n = getActiveNetwork();
        return n != null ? new Network[]{n} : new Network[0];
    }

    public NetworkInfo getActiveNetworkInfo() {
        WindowsNetProbe probe = probeWindowsNetwork();
        if (!probe.connected()) {
            return null;
        }
        int type = probe.wifi() ? TYPE_WIFI : TYPE_ETHERNET;
        String typeName = probe.wifi() ? "WIFI" : "ETHERNET";
        NetworkInfo info = new NetworkInfo(type, 0, typeName, "");
        info.setIsAvailable(true);
        info.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, probe.ifaceName());
        return info;
    }

    public NetworkInfo getNetworkInfo(Network network) {
        return network != null ? getActiveNetworkInfo() : null;
    }

    public NetworkInfo getNetworkInfo(int networkType) {
        NetworkInfo active = getActiveNetworkInfo();
        if (active != null && active.getType() == networkType) {
            return active;
        }
        if (networkType == TYPE_WIFI && active != null) {
            // Trên PC chạy Ethernet, nhiều app cũ hỏi getNetworkInfo(TYPE_WIFI).isConnected():
            // Nếu đang online qua Ethernet, vẫn trả về kết nối khả dụng khi networkPreference == TYPE_WIFI.
            NetworkInfo wifiAlias = new NetworkInfo(TYPE_WIFI, 0, "WIFI", "");
            wifiAlias.setIsAvailable(true);
            wifiAlias.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, active.getExtraInfo());
            return wifiAlias;
        }
        NetworkInfo offline = new NetworkInfo(networkType, 0, networkType == TYPE_WIFI ? "WIFI" : "MOBILE", "");
        offline.setIsAvailable(false);
        offline.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        return offline;
    }

    public NetworkInfo[] getAllNetworkInfo() {
        NetworkInfo active = getActiveNetworkInfo();
        return active != null ? new NetworkInfo[]{active} : new NetworkInfo[0];
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        if (network == null) return null;
        WindowsNetProbe probe = probeWindowsNetwork();
        if (!probe.connected()) return null;
        NetworkCapabilities nc = new NetworkCapabilities();
        // Khai báo cả TRANSPORT_WIFI và TRANSPORT_ETHERNET nếu máy Windows nối dây để app chỉ lọc TRANSPORT_WIFI vẫn nhận mạng
        nc.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        if (!probe.wifi()) {
            nc.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        }
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
        nc.setTransportInfo(new WifiInfo());
        return nc;
    }

    public LinkProperties getActiveLinkProperties() {
        return getLinkProperties(getActiveNetwork());
    }

    public LinkProperties getLinkProperties(Network network) {
        if (network == null) return null;
        WindowsNetProbe probe = probeWindowsNetwork();
        if (!probe.connected()) return null;
        LinkProperties lp = new LinkProperties();
        String ifName = probe.sysIfaceName() != null ? probe.sysIfaceName() : "wlan0";
        lp.setInterfaceName(ifName);
        lp.setMtu(probe.mtu());
        lp.setLinkAddresses(probe.linkAddresses());
        try {
            lp.addDnsServer(InetAddress.getByAddress(new byte[]{8, 8, 8, 8}));
            lp.addDnsServer(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}));
            IpPrefix defaultDest = new IpPrefix(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 0);
            InetAddress gw = InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
            lp.addRoute(new RouteInfo(defaultDest, gw, ifName, RouteInfo.RTN_UNICAST));
        } catch (Exception ignored) {
        }
        return lp;
    }

    public ProxyInfo getDefaultProxy() {
        return null;
    }

    public boolean isActiveNetworkMetered() {
        return false;
    }

    public boolean isDefaultNetworkActive() {
        return probeWindowsNetwork().connected();
    }

    public boolean getBackgroundDataSetting() {
        return true;
    }

    public int getRestrictBackgroundStatus() {
        return RESTRICT_BACKGROUND_STATUS_DISABLED;
    }

    public int getNetworkPreference() {
        return networkPreference;
    }

    public void setNetworkPreference(int preference) {
        this.networkPreference = preference;
    }

    public boolean bindProcessToNetwork(Network network) {
        processBoundNetwork = network;
        return true;
    }

    public Network getBoundNetworkForProcess() {
        return processBoundNetwork;
    }

    public static boolean setProcessDefaultNetwork(Network network) {
        processBoundNetwork = network;
        return true;
    }

    public static Network getProcessDefaultNetwork() {
        return processBoundNetwork;
    }

    public int getMultipathPreference(Network network) {
        return MULTIPATH_PREFERENCE_PERFORMANCE;
    }

    public void reportBadNetwork(Network network) {
    }

    public void reportNetworkConnectivity(Network network, boolean hasConnectivity) {
    }

    public boolean requestBandwidthUpdate(Network network) {
        return true;
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback) {
        registerDefaultNetworkCallback(networkCallback, null);
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback, Handler handler) {
        registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback, handler);
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback) {
        registerNetworkCallback(request, networkCallback, null);
    }

    public void registerNetworkCallback(NetworkRequest request, PendingIntent operation) {
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback, Handler handler) {
        if (networkCallback == null) return;
        CallbackRegistration reg = new CallbackRegistration(request, networkCallback, handler);
        registeredCallbacks.put(networkCallback, reg);
        dispatchCallbackForCurrentState(reg, false);
    }

    public void registerBestMatchingNetworkCallback(NetworkRequest request, NetworkCallback networkCallback, Handler handler) {
        registerNetworkCallback(request, networkCallback, handler);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback) {
        registerNetworkCallback(request, networkCallback, null);
    }

    public void requestNetwork(NetworkRequest request, PendingIntent operation) {
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler) {
        registerNetworkCallback(request, networkCallback, handler);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, int timeoutMs) {
        requestNetwork(request, networkCallback, null, timeoutMs);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler, int timeoutMs) {
        if (networkCallback == null) return;
        CallbackRegistration reg = new CallbackRegistration(request, networkCallback, handler);
        registeredCallbacks.put(networkCallback, reg);
        dispatchCallbackForCurrentState(reg, true);
    }

    public void reserveNetwork(NetworkRequest request, Handler handler, NetworkCallback networkCallback) {
        requestNetwork(request, networkCallback, handler);
    }

    private void dispatchCallbackForCurrentState(CallbackRegistration reg, boolean notifyUnavailable) {
        Runnable notify = () -> {
            Network net = getActiveNetwork();
            NetworkCapabilities nc = getNetworkCapabilities(net);
            LinkProperties lp = getLinkProperties(net);
            if (net != null && nc != null && (reg.request == null || reg.request.canBeSatisfiedBy(nc))) {
                reg.callback.onAvailable(net);
                reg.callback.onCapabilitiesChanged(net, nc);
                if (lp != null) {
                    reg.callback.onLinkPropertiesChanged(net, lp);
                }
                reg.callback.onBlockedStatusChanged(net, false);
            } else if (notifyUnavailable) {
                reg.callback.onUnavailable();
            }
        };
        if (reg.handler != null) {
            reg.handler.post(notify);
        } else {
            notify.run();
        }
    }

    /**
     * Chủ động phát sự kiện thay đổi trạng thái mạng tới toàn bộ NetworkCallback đã đăng ký.
     */
    public void notifyNetworkStateChanged() {
        Network net = getActiveNetwork();
        NetworkCapabilities nc = getNetworkCapabilities(net);
        LinkProperties lp = getLinkProperties(net);
        Network fallbackLostNet = new Network(100);

        for (CallbackRegistration reg : registeredCallbacks.values()) {
            Runnable action = () -> {
                if (net != null && nc != null && (reg.request == null || reg.request.canBeSatisfiedBy(nc))) {
                    reg.callback.onAvailable(net);
                    reg.callback.onCapabilitiesChanged(net, nc);
                    if (lp != null) {
                        reg.callback.onLinkPropertiesChanged(net, lp);
                    }
                } else {
                    reg.callback.onLost(fallbackLostNet);
                }
            };
            if (reg.handler != null) {
                reg.handler.post(action);
            } else {
                action.run();
            }
        }
        if (net != null) {
            for (OnNetworkActiveListener l : activeListeners) {
                l.onNetworkActive();
            }
        }
    }

    public void unregisterNetworkCallback(NetworkCallback networkCallback) {
        if (networkCallback != null) {
            registeredCallbacks.remove(networkCallback);
        }
    }

    public void unregisterNetworkCallback(PendingIntent operation) {
    }

    public void addDefaultNetworkActiveListener(OnNetworkActiveListener l) {
        if (l != null) activeListeners.add(l);
    }

    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener l) {
        if (l != null) activeListeners.remove(l);
    }
}
