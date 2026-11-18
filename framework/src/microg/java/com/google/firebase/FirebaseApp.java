package com.google.firebase;

import android.app.ActivityThread;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp thay thế nhẹ cho com.google.firebase.FirebaseApp (microG-lite).
 * Tự động khởi tạo an toàn khi ứng dụng gọi getInstance() hoặc qua FirebaseInitProvider.
 */
public class FirebaseApp {
    public static final String DEFAULT_APP_NAME = "[DEFAULT]";
    private static final Map<String, FirebaseApp> INSTANCES = new ConcurrentHashMap<>();

    public interface BackgroundStateChangeListener {
        void onBackgroundStateChanged(boolean background);
    }

    private final Context applicationContext;
    private final String name;
    private final FirebaseOptions options;
    private boolean dataCollectionDefaultEnabled = true;
    private boolean deleted;

    private FirebaseApp(Context applicationContext, String name, FirebaseOptions options) {
        this.applicationContext = applicationContext;
        this.name = name;
        this.options = options;
    }

    public static List<FirebaseApp> getApps(Context context) {
        return new ArrayList<>(INSTANCES.values());
    }

    public static FirebaseApp getInstance() {
        FirebaseApp app = INSTANCES.get(DEFAULT_APP_NAME);
        if (app == null) {
            Context ctx = ActivityThread.currentApplication();
            if (ctx != null) {
                app = initializeApp(ctx);
            }
        }
        if (app == null) {
            app = initializeApp(null, new FirebaseOptions.Builder().build(), DEFAULT_APP_NAME);
        }
        return app;
    }

    public static FirebaseApp getInstance(String name) {
        FirebaseApp app = INSTANCES.get(name);
        if (app != null) return app;
        if (DEFAULT_APP_NAME.equals(name)) return getInstance();
        throw new IllegalStateException("FirebaseApp with name " + name + " doesn't exist.");
    }

    public static FirebaseApp initializeApp(Context context) {
        FirebaseApp existing = INSTANCES.get(DEFAULT_APP_NAME);
        if (existing != null) return existing;
        FirebaseOptions opts = FirebaseOptions.fromResource(context);
        if (opts == null) opts = new FirebaseOptions.Builder().build();
        return initializeApp(context, opts, DEFAULT_APP_NAME);
    }

    public static FirebaseApp initializeApp(Context context, FirebaseOptions options) {
        return initializeApp(context, options, DEFAULT_APP_NAME);
    }

    public static FirebaseApp initializeApp(Context context, FirebaseOptions options, String name) {
        String key = (name == null || name.trim().isEmpty()) ? DEFAULT_APP_NAME : name.trim();
        Context appCtx = (context != null && context.getApplicationContext() != null)
                ? context.getApplicationContext() : context;
        FirebaseOptions finalOpts = options != null ? options : new FirebaseOptions.Builder().build();
        return INSTANCES.computeIfAbsent(key, k -> new FirebaseApp(appCtx, k, finalOpts));
    }

    public static void clearInstancesForTest() {
        INSTANCES.clear();
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public String getName() {
        return name;
    }

    public FirebaseOptions getOptions() {
        return options;
    }

    public boolean isDefaultApp() {
        return DEFAULT_APP_NAME.equals(name);
    }

    public boolean isDataCollectionDefaultEnabled() {
        return dataCollectionDefaultEnabled;
    }

    public void setDataCollectionDefaultEnabled(Boolean enabled) {
        this.dataCollectionDefaultEnabled = enabled != null && enabled;
    }

    public void setDataCollectionDefaultEnabled(boolean enabled) {
        this.dataCollectionDefaultEnabled = enabled;
    }

    public void setAutomaticResourceManagementEnabled(boolean enabled) {
    }

    public void addBackgroundStateChangeListener(BackgroundStateChangeListener listener) {
    }

    public void removeBackgroundStateChangeListener(BackgroundStateChangeListener listener) {
    }

    public void delete() {
        if (!deleted) {
            deleted = true;
            INSTANCES.remove(name, this);
        }
    }

    public String getPersistenceKey() {
        return name + "+" + options.getApplicationId();
    }
}
