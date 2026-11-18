package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Yêu cầu mạng dùng với ConnectivityManager.registerNetworkCallback() / requestNetwork(). */
public class NetworkRequest implements Parcelable {
    private final Set<Integer> capabilities = new LinkedHashSet<>();
    private final Set<Integer> transports = new LinkedHashSet<>();

    NetworkRequest() {
    }

    public NetworkRequest(NetworkRequest that) {
        if (that != null) {
            this.capabilities.addAll(that.capabilities);
            this.transports.addAll(that.transports);
        }
    }

    public boolean hasCapability(int capability) {
        return capabilities.contains(capability);
    }

    public boolean hasTransport(int transportType) {
        return transports.contains(transportType);
    }

    public int[] getCapabilities() {
        return capabilities.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getTransportTypes() {
        return transports.stream().mapToInt(Integer::intValue).toArray();
    }

    public Set<Integer> getSubscriptionIds() {
        return Collections.emptySet();
    }

    public boolean canBeSatisfiedBy(NetworkCapabilities nc) {
        if (nc == null) return false;
        for (int cap : capabilities) {
            if (!nc.hasCapability(cap)) return false;
        }
        if (!transports.isEmpty()) {
            boolean any = false;
            for (int tr : transports) {
                if (nc.hasTransport(tr)) {
                    any = true;
                    break;
                }
            }
            if (!any) return false;
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(getCapabilities());
        dest.writeIntArray(getTransportTypes());
    }

    public static final Creator<NetworkRequest> CREATOR = new Creator<>() {
        @Override
        public NetworkRequest createFromParcel(Parcel in) {
            NetworkRequest nr = new NetworkRequest();
            for (int c : in.createIntArray()) nr.capabilities.add(c);
            for (int t : in.createIntArray()) nr.transports.add(t);
            return nr;
        }

        @Override
        public NetworkRequest[] newArray(int size) {
            return new NetworkRequest[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof NetworkRequest other
                && Objects.equals(capabilities, other.capabilities)
                && Objects.equals(transports, other.transports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilities, transports);
    }

    public static class Builder {
        private final NetworkRequest request;

        public Builder() {
            this.request = new NetworkRequest();
            this.request.capabilities.add(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            this.request.capabilities.add(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            this.request.capabilities.add(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
        }

        public Builder(NetworkRequest request) {
            this.request = new NetworkRequest(request);
        }

        public Builder addCapability(int capability) {
            request.capabilities.add(capability);
            return this;
        }

        public Builder removeCapability(int capability) {
            request.capabilities.remove(capability);
            return this;
        }

        public Builder clearCapabilities() {
            request.capabilities.clear();
            return this;
        }

        public Builder addTransportType(int transportType) {
            request.transports.add(transportType);
            return this;
        }

        public Builder removeTransportType(int transportType) {
            request.transports.remove(transportType);
            return this;
        }

        public Builder setIncludeOtherUidNetworks(boolean include) {
            return this;
        }

        public Builder setNetworkSpecifier(String networkSpecifier) {
            return this;
        }

        public Builder setSubscriptionIds(Set<Integer> subIds) {
            return this;
        }

        public NetworkRequest build() {
            return new NetworkRequest(request);
        }
    }
}
