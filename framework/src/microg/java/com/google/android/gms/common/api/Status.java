package com.google.android.gms.common.api;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

public final class Status implements Parcelable {
    public static final Status RESULT_SUCCESS = new Status(0, "SUCCESS");
    public static final Status RESULT_CANCELED = new Status(16, "CANCELED");
    public static final Status RESULT_INTERNAL_ERROR = new Status(8, "INTERNAL_ERROR");

    private final int statusCode;
    private final String statusMessage;
    private final PendingIntent pendingIntent;

    public Status(int statusCode) {
        this(statusCode, null, null);
    }

    public Status(int statusCode, String statusMessage) {
        this(statusCode, statusMessage, null);
    }

    public Status(int statusCode, String statusMessage, PendingIntent pendingIntent) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.pendingIntent = pendingIntent;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public PendingIntent getResolution() {
        return pendingIntent;
    }

    public boolean isSuccess() {
        return statusCode <= 0;
    }

    public boolean isCanceled() {
        return statusCode == 16;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(statusCode);
        dest.writeString(statusMessage);
        dest.writeParcelable(pendingIntent, flags);
    }

    public static final Creator<Status> CREATOR = new Creator<>() {
        @Override
        public Status createFromParcel(Parcel source) {
            return new Status(source.readInt(), source.readString(), source.readParcelable(null));
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };
}
