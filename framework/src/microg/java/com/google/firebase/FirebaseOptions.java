package com.google.firebase;

import android.content.Context;
import android.content.res.Resources;

/** Cấu hình FirebaseOptions (đọc từ resources.arsc hoặc giá trị mặc định an toàn). */
public final class FirebaseOptions {
    private final String apiKey;
    private final String applicationId;
    private final String databaseUrl;
    private final String gaTrackingId;
    private final String gcmSenderId;
    private final String storageBucket;
    private final String projectId;

    public static final class Builder {
        private String apiKey = "AIzaSyAAOWMicroGLiteDefaultKey";
        private String applicationId = "1:000000000000:android:aaow00000000";
        private String databaseUrl;
        private String gaTrackingId;
        private String gcmSenderId = "000000000000";
        private String storageBucket;
        private String projectId = "aaow-microg-lite";

        public Builder() {
        }

        public Builder(FirebaseOptions options) {
            if (options == null) return;
            this.apiKey = options.apiKey;
            this.applicationId = options.applicationId;
            this.databaseUrl = options.databaseUrl;
            this.gaTrackingId = options.gaTrackingId;
            this.gcmSenderId = options.gcmSenderId;
            this.storageBucket = options.storageBucket;
            this.projectId = options.projectId;
        }

        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder setApplicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder setDatabaseUrl(String databaseUrl) {
            this.databaseUrl = databaseUrl;
            return this;
        }

        public Builder setGaTrackingId(String gaTrackingId) {
            this.gaTrackingId = gaTrackingId;
            return this;
        }

        public Builder setGcmSenderId(String gcmSenderId) {
            this.gcmSenderId = gcmSenderId;
            return this;
        }

        public Builder setStorageBucket(String storageBucket) {
            this.storageBucket = storageBucket;
            return this;
        }

        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public FirebaseOptions build() {
            return new FirebaseOptions(applicationId, apiKey, databaseUrl, gaTrackingId, gcmSenderId, storageBucket, projectId);
        }
    }

    private FirebaseOptions(String applicationId, String apiKey, String databaseUrl, String gaTrackingId,
                            String gcmSenderId, String storageBucket, String projectId) {
        this.applicationId = applicationId;
        this.apiKey = apiKey;
        this.databaseUrl = databaseUrl;
        this.gaTrackingId = gaTrackingId;
        this.gcmSenderId = gcmSenderId;
        this.storageBucket = storageBucket;
        this.projectId = projectId;
    }

    public static FirebaseOptions fromResource(Context context) {
        if (context == null || context.getResources() == null) return null;
        String pkg = context.getPackageName();
        Resources res = context.getResources();
        String appId = readStringRes(res, pkg, "google_app_id");
        if (appId == null || appId.isEmpty()) return null;
        Builder b = new Builder().setApplicationId(appId);
        String apiKey = readStringRes(res, pkg, "google_api_key");
        if (apiKey != null) b.setApiKey(apiKey);
        String dbUrl = readStringRes(res, pkg, "firebase_database_url");
        if (dbUrl != null) b.setDatabaseUrl(dbUrl);
        String gcm = readStringRes(res, pkg, "gcm_defaultSenderId");
        if (gcm != null) b.setGcmSenderId(gcm);
        String bucket = readStringRes(res, pkg, "google_storage_bucket");
        if (bucket != null) b.setStorageBucket(bucket);
        String proj = readStringRes(res, pkg, "project_id");
        if (proj != null) b.setProjectId(proj);
        return b.build();
    }

    private static String readStringRes(Resources res, String pkg, String key) {
        try {
            int id = res.getIdentifier(key, "string", pkg);
            return id != 0 ? res.getString(id) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getGaTrackingId() {
        return gaTrackingId;
    }

    public String getGcmSenderId() {
        return gcmSenderId;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public String getProjectId() {
        return projectId;
    }
}
