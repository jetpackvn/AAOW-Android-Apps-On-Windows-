package com.google.firebase.crashlytics;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class FirebaseCrashlytics {
    private static final FirebaseCrashlytics INSTANCE = new FirebaseCrashlytics();

    public static FirebaseCrashlytics getInstance() {
        return INSTANCE;
    }

    public void recordException(Throwable throwable) {
    }

    public void log(String message) {
    }

    public void setUserId(String identifier) {
    }

    public void setCustomKey(String key, boolean value) {
    }

    public void setCustomKey(String key, double value) {
    }

    public void setCustomKey(String key, float value) {
    }

    public void setCustomKey(String key, int value) {
    }

    public void setCustomKey(String key, long value) {
    }

    public void setCustomKey(String key, String value) {
    }

    public void setCrashlyticsCollectionEnabled(boolean enabled) {
    }

    public void setCrashlyticsCollectionEnabled(Boolean enabled) {
    }

    public boolean didCrashOnPreviousExecution() {
        return false;
    }

    public Task<Boolean> checkForUnsentReports() {
        return Tasks.forResult(Boolean.FALSE);
    }

    public void sendUnsentReports() {
    }

    public void deleteUnsentReports() {
    }
}
