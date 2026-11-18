package com.google.android.gms.security;

import android.content.Context;
import android.content.Intent;

public class ProviderInstaller {
    public interface ProviderInstallListener {
        void onProviderInstalled();

        void onProviderInstallFailed(int errorCode, Intent recoveryIntent);
    }

    public static void installIfNeeded(Context context) {
    }

    public static void installIfNeededAsync(Context context, ProviderInstallListener listener) {
        if (listener != null) {
            listener.onProviderInstalled();
        }
    }
}
