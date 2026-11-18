package com.google.android.gms.common;

import android.app.PendingIntent;

public final class ConnectionResult {
    public static final int SUCCESS = 0;
    public static final int SERVICE_MISSING = 1;
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
    public static final int SERVICE_DISABLED = 3;
    public static final int SIGN_IN_REQUIRED = 4;
    public static final int INVALID_ACCOUNT = 5;
    public static final int RESOLUTION_REQUIRED = 6;
    public static final int NETWORK_ERROR = 7;
    public static final int INTERNAL_ERROR = 8;
    public static final int SERVICE_INVALID = 9;
    public static final int DEVELOPER_ERROR = 10;

    public static final ConnectionResult RESULT_SUCCESS = new ConnectionResult(SUCCESS);

    private final int errorCode;
    private final PendingIntent resolution;
    private final String errorMessage;

    public ConnectionResult(int statusCode) {
        this(statusCode, null, null);
    }

    public ConnectionResult(int statusCode, PendingIntent pendingIntent) {
        this(statusCode, pendingIntent, null);
    }

    public ConnectionResult(int statusCode, PendingIntent pendingIntent, String message) {
        this.errorCode = statusCode;
        this.resolution = pendingIntent;
        this.errorMessage = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isSuccess() {
        return errorCode == SUCCESS;
    }

    public boolean hasResolution() {
        return errorCode != 0 && resolution != null;
    }

    public PendingIntent getResolution() {
        return resolution;
    }

    public String getErrorMessage() {
        if (errorMessage != null) return errorMessage;
        return switch (errorCode) {
            case SUCCESS -> "SUCCESS";
            case SERVICE_MISSING -> "SERVICE_MISSING";
            case SERVICE_VERSION_UPDATE_REQUIRED -> "SERVICE_VERSION_UPDATE_REQUIRED";
            case SERVICE_DISABLED -> "SERVICE_DISABLED";
            default -> "UNKNOWN_ERROR(" + errorCode + ")";
        };
    }
}
