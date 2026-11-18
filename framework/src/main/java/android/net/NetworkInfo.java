package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/** Trạng thái kết nối mạng tương thích android.net.NetworkInfo. */
public class NetworkInfo implements Parcelable {
    public enum State {
        CONNECTING, CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN
    }

    public enum DetailedState {
        IDLE, SCANNING, CONNECTING, AUTHENTICATING, OBTAINING_IPADDR,
        CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, FAILED,
        BLOCKED, VERIFYING_POOR_LINK, CAPTIVE_PORTAL_CHECK
    }

    private int networkType;
    private int subtype;
    private String typeName;
    private String subtypeName;
    private State state = State.UNKNOWN;
    private DetailedState detailedState = DetailedState.IDLE;
    private String reason;
    private String extraInfo;
    private boolean isAvailable;
    private boolean isFailover;
    private boolean isRoaming;

    public NetworkInfo(int type, int subtype, String typeName, String subtypeName) {
        this.networkType = type;
        this.subtype = subtype;
        this.typeName = typeName;
        this.subtypeName = subtypeName;
    }

    public int getType() {
        return networkType;
    }

    public int getSubtype() {
        return subtype;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getSubtypeName() {
        return subtypeName;
    }

    public boolean isConnectedOrConnecting() {
        return state == State.CONNECTED || state == State.CONNECTING;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public boolean isFailover() {
        return isFailover;
    }

    public boolean isRoaming() {
        return isRoaming;
    }

    public State getState() {
        return state;
    }

    public DetailedState getDetailedState() {
        return detailedState;
    }

    public void setDetailedState(DetailedState detailedState, String reason, String extraInfo) {
        this.detailedState = detailedState != null ? detailedState : DetailedState.IDLE;
        this.reason = reason;
        this.extraInfo = extraInfo;
        this.state = switch (this.detailedState) {
            case CONNECTED -> State.CONNECTED;
            case CONNECTING, AUTHENTICATING, OBTAINING_IPADDR, SCANNING, VERIFYING_POOR_LINK, CAPTIVE_PORTAL_CHECK -> State.CONNECTING;
            case SUSPENDED -> State.SUSPENDED;
            case DISCONNECTING -> State.DISCONNECTING;
            case DISCONNECTED, FAILED, BLOCKED -> State.DISCONNECTED;
            case IDLE -> State.UNKNOWN;
        };
    }

    public String getReason() {
        return reason;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(networkType);
        dest.writeInt(subtype);
        dest.writeString(typeName);
        dest.writeString(subtypeName);
        dest.writeString(state.name());
        dest.writeString(detailedState.name());
        dest.writeBoolean(isAvailable);
        dest.writeBoolean(isFailover);
        dest.writeBoolean(isRoaming);
        dest.writeString(reason);
        dest.writeString(extraInfo);
    }

    public static final Creator<NetworkInfo> CREATOR = new Creator<>() {
        @Override
        public NetworkInfo createFromParcel(Parcel in) {
            NetworkInfo ni = new NetworkInfo(in.readInt(), in.readInt(), in.readString(), in.readString());
            ni.state = State.valueOf(in.readString());
            ni.detailedState = DetailedState.valueOf(in.readString());
            ni.isAvailable = in.readBoolean();
            ni.isFailover = in.readBoolean();
            ni.isRoaming = in.readBoolean();
            ni.reason = in.readString();
            ni.extraInfo = in.readString();
            return ni;
        }

        @Override
        public NetworkInfo[] newArray(int size) {
            return new NetworkInfo[size];
        }
    };

    @Override
    public String toString() {
        return "[type: " + typeName + ", state: " + state + "/" + detailedState + ", available: " + isAvailable + "]";
    }
}
