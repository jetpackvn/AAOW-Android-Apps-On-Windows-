package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

/** Tiền tố địa chỉ IP (IPv4/IPv6 CIDR) dùng trong RouteInfo và LinkProperties. */
public final class IpPrefix implements Parcelable {
    private final byte[] address;
    private final int prefixLength;

    public IpPrefix(InetAddress address, int prefixLength) {
        if (address == null) {
            throw new NullPointerException("address must not be null");
        }
        byte[] raw = address.getAddress();
        if (prefixLength < 0 || prefixLength > raw.length * 8) {
            throw new IllegalArgumentException("Invalid prefix length " + prefixLength);
        }
        this.address = maskAddress(raw, prefixLength);
        this.prefixLength = prefixLength;
    }

    public IpPrefix(byte[] address, int prefixLength) {
        if (address == null) {
            throw new NullPointerException("address must not be null");
        }
        if (address.length != 4 && address.length != 16) {
            throw new IllegalArgumentException("Invalid IP address length: " + address.length);
        }
        if (prefixLength < 0 || prefixLength > address.length * 8) {
            throw new IllegalArgumentException("Invalid prefix length " + prefixLength);
        }
        this.address = maskAddress(address, prefixLength);
        this.prefixLength = prefixLength;
    }

    private static byte[] maskAddress(byte[] raw, int prefixLen) {
        byte[] copy = raw.clone();
        int fullBytes = prefixLen / 8;
        int remBits = prefixLen % 8;
        if (fullBytes < copy.length) {
            if (remBits != 0) {
                int mask = (0xFF << (8 - remBits)) & 0xFF;
                copy[fullBytes] = (byte) (copy[fullBytes] & mask);
                fullBytes++;
            }
            for (int i = fullBytes; i < copy.length; i++) {
                copy[i] = 0;
            }
        }
        return copy;
    }

    public InetAddress getAddress() {
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public byte[] getRawAddress() {
        return address.clone();
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public boolean contains(InetAddress addr) {
        if (addr == null) return false;
        byte[] other = addr.getAddress();
        if (other.length != address.length) return false;
        byte[] masked = maskAddress(other, prefixLength);
        return Arrays.equals(address, masked);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(address);
        dest.writeInt(prefixLength);
    }

    public static final Creator<IpPrefix> CREATOR = new Creator<>() {
        @Override
        public IpPrefix createFromParcel(Parcel in) {
            byte[] raw = in.createByteArray();
            int len = in.readInt();
            return new IpPrefix(raw != null ? raw : new byte[4], len);
        }

        @Override
        public IpPrefix[] newArray(int size) {
            return new IpPrefix[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof IpPrefix other
                && prefixLength == other.prefixLength
                && Arrays.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(address), prefixLength);
    }

    @Override
    public String toString() {
        InetAddress ia = getAddress();
        return (ia != null ? ia.getHostAddress() : "0.0.0.0") + "/" + prefixLength;
    }
}
