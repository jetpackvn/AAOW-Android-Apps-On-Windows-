package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Cấu hình HTTP Proxy cho một kết nối mạng (LinkProperties). */
public class ProxyInfo implements Parcelable {
    private final String host;
    private final int port;
    private final String[] exclusionList;
    private final Uri pacFileUrl;

    private ProxyInfo(String host, int port, List<String> exclList, Uri pacFileUrl) {
        this.host = host;
        this.port = port;
        this.exclusionList = exclList != null ? exclList.toArray(new String[0]) : new String[0];
        this.pacFileUrl = pacFileUrl != null ? pacFileUrl : Uri.EMPTY;
    }

    public ProxyInfo(ProxyInfo source) {
        if (source != null) {
            this.host = source.host;
            this.port = source.port;
            this.exclusionList = source.exclusionList != null ? source.exclusionList.clone() : new String[0];
            this.pacFileUrl = source.pacFileUrl;
        } else {
            this.host = null;
            this.port = 0;
            this.exclusionList = new String[0];
            this.pacFileUrl = Uri.EMPTY;
        }
    }

    public static ProxyInfo buildDirectProxy(String host, int port) {
        return new ProxyInfo(host, port, null, Uri.EMPTY);
    }

    public static ProxyInfo buildDirectProxy(String host, int port, List<String> exclList) {
        return new ProxyInfo(host, port, exclList, Uri.EMPTY);
    }

    public static ProxyInfo buildPacProxy(Uri pacUri) {
        return new ProxyInfo("localhost", -1, null, pacUri);
    }

    public static ProxyInfo buildPacProxy(Uri pacUri, int port) {
        return new ProxyInfo("localhost", port, null, pacUri);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String[] getExclusionList() {
        return exclusionList != null ? exclusionList.clone() : new String[0];
    }

    public Uri getPacFileUrl() {
        return pacFileUrl;
    }

    public boolean isValid() {
        if (pacFileUrl != null && !Uri.EMPTY.equals(pacFileUrl)) return true;
        return host != null && !host.isEmpty() && port > 0 && port <= 65535;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeStringArray(exclusionList);
        dest.writeString(pacFileUrl != null ? pacFileUrl.toString() : "");
    }

    public static final Creator<ProxyInfo> CREATOR = new Creator<>() {
        @Override
        public ProxyInfo createFromParcel(Parcel in) {
            String h = in.readString();
            int p = in.readInt();
            String[] excl = in.createStringArray();
            String pacStr = in.readString();
            Uri pac = (pacStr != null && !pacStr.isEmpty()) ? Uri.parse(pacStr) : Uri.EMPTY;
            return new ProxyInfo(h, p, excl != null ? Arrays.asList(excl) : null, pac);
        }

        @Override
        public ProxyInfo[] newArray(int size) {
            return new ProxyInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof ProxyInfo other
                && port == other.port
                && Objects.equals(host, other.host)
                && Arrays.equals(exclusionList, other.exclusionList)
                && Objects.equals(pacFileUrl, other.pacFileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, Arrays.hashCode(exclusionList), pacFileUrl);
    }

    @Override
    public String toString() {
        if (pacFileUrl != null && !Uri.EMPTY.equals(pacFileUrl)) {
            return "PAC: " + pacFileUrl;
        }
        return "[" + (host != null ? host : "DIRECT") + ":" + port + "]";
    }
}
