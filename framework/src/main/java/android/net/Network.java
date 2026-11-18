package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import javax.net.SocketFactory;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/** Đại diện cho một kết nối mạng đang hoạt động trên Windows. */
public class Network implements Parcelable {
    public final int netId;

    public Network(int netId) {
        this.netId = netId;
    }

    public static Network fromNetworkHandle(long networkHandle) {
        if (networkHandle == 0) {
            throw new IllegalArgumentException("Network.fromNetworkHandle refusing to instantiate NETID_UNSET");
        }
        return new Network((int) (networkHandle >>> 32));
    }

    public long getNetworkHandle() {
        return ((long) netId << 32) | 0x0000cafeL;
    }

    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    public InetAddress getByName(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    public SocketFactory getSocketFactory() {
        return SocketFactory.getDefault();
    }

    public URLConnection openConnection(URL url) throws IOException {
        return url.openConnection();
    }

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return proxy != null ? url.openConnection(proxy) : url.openConnection();
    }

    public void bindSocket(DatagramSocket socket) throws IOException {
    }

    public void bindSocket(Socket socket) throws IOException {
    }

    public void bindSocket(FileDescriptor fd) throws IOException {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(netId);
    }

    public static final Creator<Network> CREATOR = new Creator<>() {
        @Override
        public Network createFromParcel(Parcel in) {
            return new Network(in.readInt());
        }

        @Override
        public Network[] newArray(int size) {
            return new Network[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Network other && netId == other.netId;
    }

    @Override
    public int hashCode() {
        return netId * 11;
    }

    @Override
    public String toString() {
        return Integer.toString(netId);
    }
}
