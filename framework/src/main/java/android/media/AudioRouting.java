package android.media;

import android.os.Handler;

public interface AudioRouting {
    interface OnRoutingChangedListener {
        void onRoutingChanged(AudioRouting router);
    }

    default boolean setPreferredDevice(Object deviceInfo) {
        return false;
    }

    default Object getPreferredDevice() {
        return null;
    }

    default Object getRoutedDevice() {
        return null;
    }

    default void addOnRoutingChangedListener(OnRoutingChangedListener listener, Handler handler) {
    }

    default void removeOnRoutingChangedListener(OnRoutingChangedListener listener) {
    }
}
