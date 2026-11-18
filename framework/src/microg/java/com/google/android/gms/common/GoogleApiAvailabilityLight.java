package com.google.android.gms.common;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import dev.aaow.rt.microg.MicroGServiceRegistry;

public class GoogleApiAvailabilityLight {
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = 12451000;
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = MicroGServiceRegistry.GMS_PACKAGE;

    private static final GoogleApiAvailabilityLight INSTANCE = new GoogleApiAvailabilityLight();

    public static GoogleApiAvailabilityLight getInstance() {
        return INSTANCE;
    }

    public int isGooglePlayServicesAvailable(Context context) {
        return isGooglePlayServicesAvailable(context, GOOGLE_PLAY_SERVICES_VERSION_CODE);
    }

    public int isGooglePlayServicesAvailable(Context context, int minApkVersion) {
        if (minApkVersion > MicroGServiceRegistry.GMS_VERSION_CODE) {
            return ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
        }
        return ConnectionResult.SUCCESS;
    }

    public int getApkVersion(Context context) {
        return MicroGServiceRegistry.GMS_VERSION_CODE;
    }

    public int getClientVersion(Context context) {
        return GOOGLE_PLAY_SERVICES_VERSION_CODE;
    }

    public boolean isUserResolvableError(int errorCode) {
        return errorCode == ConnectionResult.SERVICE_MISSING
                || errorCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
                || errorCode == ConnectionResult.SERVICE_DISABLED;
    }

    public Intent getErrorResolutionIntent(Context context, int errorCode, String track) {
        return null;
    }

    public PendingIntent getErrorResolutionPendingIntent(Context context, int errorCode, int requestCode) {
        return null;
    }

    public String getErrorString(int errorCode) {
        return new ConnectionResult(errorCode).getErrorMessage();
    }
}
