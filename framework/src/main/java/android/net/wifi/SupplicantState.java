package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

/** Trạng thái kết nối WPA Supplicant của WifiInfo. */
public enum SupplicantState implements Parcelable {
    DISCONNECTED,
    INTERFACE_DISABLED,
    INACTIVE,
    SCANNING,
    AUTHENTICATING,
    ASSOCIATING,
    ASSOCIATED,
    FOUR_WAY_HANDSHAKE,
    GROUP_HANDSHAKE,
    COMPLETED,
    DORMANT,
    UNINITIALIZED,
    INVALID;

    public static boolean isValidState(SupplicantState state) {
        return state != null && state != UNINITIALIZED && state != INVALID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }

    public static final Creator<SupplicantState> CREATOR = new Creator<>() {
        @Override
        public SupplicantState createFromParcel(Parcel in) {
            String s = in.readString();
            try {
                return s != null ? SupplicantState.valueOf(s) : COMPLETED;
            } catch (IllegalArgumentException e) {
                return COMPLETED;
            }
        }

        @Override
        public SupplicantState[] newArray(int size) {
            return new SupplicantState[size];
        }
    };
}
