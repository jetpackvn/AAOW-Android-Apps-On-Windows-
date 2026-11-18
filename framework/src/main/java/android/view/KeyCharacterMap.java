package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public class KeyCharacterMap implements Parcelable {
    public static final int BUILT_IN_KEYBOARD = 0;
    public static final int VIRTUAL_KEYBOARD = -1;
    public static final int NUMERIC = 1;
    public static final int PREDICTIVE = 2;
    public static final int ALPHA = 3;
    public static final int FULL = 4;
    public static final int SPECIAL_FUNCTION = 5;

    private static final KeyCharacterMap DEFAULT_INSTANCE = new KeyCharacterMap();

    KeyCharacterMap() {
    }

    public static KeyCharacterMap load(int deviceId) {
        return DEFAULT_INSTANCE;
    }

    public int getKeyboardType() {
        return FULL;
    }

    public char getDisplayLabel(int keyCode) {
        return 0;
    }

    public int get(int keyCode, int metaState) {
        return 0;
    }

    public static boolean deviceHasKey(int keyCode) {
        return false;
    }

    public static boolean[] deviceHasKeys(int[] keyCodes) {
        return new boolean[keyCodes != null ? keyCodes.length : 0];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<KeyCharacterMap> CREATOR = new Creator<>() {
        @Override
        public KeyCharacterMap createFromParcel(Parcel source) {
            return DEFAULT_INSTANCE;
        }

        @Override
        public KeyCharacterMap[] newArray(int size) {
            return new KeyCharacterMap[size];
        }
    };
}
