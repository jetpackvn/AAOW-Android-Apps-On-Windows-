package com.google.android.gms.common;

import android.content.Context;
import android.content.res.Resources;

public final class GooglePlayServicesUtil {
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;

    private GooglePlayServicesUtil() {
    }

    public static int isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
    }

    public static int isGooglePlayServicesAvailable(Context context, int minApkVersion) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context, minApkVersion);
    }

    public static boolean isUserRecoverableError(int errorCode) {
        return GoogleApiAvailability.getInstance().isUserResolvableError(errorCode);
    }

    public static String getErrorString(int errorCode) {
        return GoogleApiAvailability.getInstance().getErrorString(errorCode);
    }

    public static Context getRemoteContext(Context context) {
        return context;
    }

    public static Resources getRemoteResource(Context context) {
        return context != null ? context.getResources() : null;
    }
}
