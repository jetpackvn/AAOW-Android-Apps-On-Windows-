package com.google.firebase.installations;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;

public class FirebaseInstallations {
    private static final FirebaseInstallations INSTANCE = new FirebaseInstallations();

    public static FirebaseInstallations getInstance() {
        return INSTANCE;
    }

    public static FirebaseInstallations getInstance(FirebaseApp app) {
        return INSTANCE;
    }

    public Task<String> getId() {
        return Tasks.forResult("aaow-fid-installation-id");
    }

    public Task<Void> delete() {
        return Tasks.forResult(null);
    }
}
