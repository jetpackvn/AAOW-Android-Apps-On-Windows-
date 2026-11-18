package com.google.firebase.messaging;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class FirebaseMessaging {
    private static final FirebaseMessaging INSTANCE = new FirebaseMessaging();
    private boolean autoInitEnabled = true;

    public static synchronized FirebaseMessaging getInstance() {
        return INSTANCE;
    }

    public boolean isAutoInitEnabled() {
        return autoInitEnabled;
    }

    public void setAutoInitEnabled(boolean enable) {
        this.autoInitEnabled = enable;
    }

    public Task<String> getToken() {
        return Tasks.forResult("aaow-fcm-dummy-token");
    }

    public Task<Void> deleteToken() {
        return Tasks.forResult(null);
    }

    public Task<Void> subscribeToTopic(String topic) {
        return Tasks.forResult(null);
    }

    public Task<Void> unsubscribeFromTopic(String topic) {
        return Tasks.forResult(null);
    }
}
