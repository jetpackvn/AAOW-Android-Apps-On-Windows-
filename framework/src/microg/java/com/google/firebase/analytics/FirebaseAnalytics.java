package com.google.firebase.analytics;

import android.content.Context;
import android.os.Bundle;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public final class FirebaseAnalytics {
    private static final FirebaseAnalytics INSTANCE = new FirebaseAnalytics();

    private FirebaseAnalytics() {
    }

    public static FirebaseAnalytics getInstance(Context context) {
        return INSTANCE;
    }

    public void logEvent(String name, Bundle params) {
    }

    public void setUserProperty(String name, String value) {
    }

    public void setUserId(String id) {
    }

    public void setAnalyticsCollectionEnabled(boolean enabled) {
    }

    public void setDefaultEventParameters(Bundle parameters) {
    }

    public void resetAnalyticsData() {
    }

    public Task<String> getAppInstanceId() {
        return Tasks.forResult("aaow-firebase-analytics-id");
    }
}
