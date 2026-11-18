package com.google.android.gms.common.api;

public class ApiException extends Exception {
    protected final Status mStatus;

    public ApiException(Status status) {
        super(status != null ? (status.getStatusCode() + ": " + status.getStatusMessage()) : "ApiException");
        this.mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
    }

    public int getStatusCode() {
        return mStatus != null ? mStatus.getStatusCode() : -1;
    }
}
