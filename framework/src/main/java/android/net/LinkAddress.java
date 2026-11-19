package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/** Địa chỉ IP kèm độ dài prefix của một giao diện mạng trên Windows. */
public class LinkAddress implements Parcelable {
    private final InetAddress address;
    private final int prefixLength;
    private final int flags;
    private final int scope;

    public LinkAddress(InetAddress address, int prefixLength) {
        this(address, prefixLength, 0, 0);
    }

    public LinkAddress(InetAddress address, int prefixLength, int flags, int scope) {
        this.address = address;
        this.prefixLength = prefixLength;
        this.flags = flags;
        this.scope = scope;
    }

    public LinkAddress(InterfaceAddress interfaceAddress) {
        this(
                interfaceAddress != null ? interfaceAddress.getAddress() : null,
                interfaceAddress != null ? Math.max(0, interfaceAddress.getNetworkPrefixLength()) : 24,
                0,
                0
        );
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public int getFlags() {
        return flags;
    }

    public int getScope() {
        return scope;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelFlags) {
        dest.writeByteArray(address != null ? address.getAddress() : new byte[]{127, 0, 0, 1});
        dest.writeInt(prefixLength);
        dest.writeInt(flags);
        dest.writeInt(scope);
    }

    public static final Creator<LinkAddress> CREATOR = new Creator<>() {
        @Override
        public LinkAddress createFromParcel(Parcel in) {
            byte[] raw = in.createByteArray();
            int len = in.readInt();
            int flg = in.readInt();
            int scp = in.readInt();
            InetAddress addr = null;
            try {
                if (raw != null) addr = InetAddress.getByAddress(raw);
            } catch (UnknownHostException ignored) {
            }
            return new LinkAddress(addr, len, flg, scp);
        }

        @Override
        public LinkAddress[] newArray(int size) {
            return new LinkAddress[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkAddress other
                && prefixLength == other.prefixLength
                && flags == other.flags
                && scope == other.scope
                && Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLength, flags, scope);
    }

    @Override
    public String toString() {
        return (address != null ? address.getHostAddress() : "0.0.0.0") + "/" + prefixLength;
    }
}
