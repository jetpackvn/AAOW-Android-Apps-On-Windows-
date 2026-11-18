package dev.aaow.rt;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Cài đặt gốc của android.content.Context cho một APK đang chạy trên Windows.
 */
public final class AndroidRuntimeContext extends Context {
    private final ApkFile apk;
    private final AndroidManifest manifest;
    private final Resources resources;
    private final Resources.Theme theme;
    private final ApplicationInfo appInfo;
    private final PackageManager packageManager;
    private final ContentResolver contentResolver;
    private final WindowManager windowManager;
    private final ClassLoader classLoader;
    private final Path dataDir;
    private final Map<String, SharedPreferences> prefsCache = new ConcurrentHashMap<>();
    private final List<RegisteredReceiver> receivers = new CopyOnWriteArrayList<>();
    private Context applicationContext = this;
    private Consumer<Intent> activityStarter;

    private record RegisteredReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    }

    public AndroidRuntimeContext(ApkFile apk, ClassLoader classLoader, Path dataRoot) {
        this.apk = apk;
        this.manifest = apk.getManifest();
        this.resources = new Resources(apk);
        this.theme = resources.newTheme();
        if (manifest.themeRes != 0) {
            this.theme.applyStyle(manifest.themeRes, true);
        }
        this.classLoader = classLoader;
        this.dataDir = dataRoot.resolve(manifest.packageName.isEmpty() ? "default_app" : manifest.packageName);
        try {
            Files.createDirectories(dataDir.resolve("files"));
            Files.createDirectories(dataDir.resolve("cache"));
            Files.createDirectories(dataDir.resolve("shared_prefs"));
        } catch (Exception ignored) {
        }

        this.appInfo = new ApplicationInfo();
        this.appInfo.packageName = manifest.packageName;
        this.appInfo.name = manifest.applicationClass;
        this.appInfo.sourceDir = apk.getPath().toAbsolutePath().toString();
        this.appInfo.dataDir = dataDir.toAbsolutePath().toString();
        this.appInfo.icon = manifest.iconRes;
        this.appInfo.theme = manifest.themeRes;
        this.appInfo.nonLocalizedLabel = manifest.label;
        this.appInfo.minSdkVersion = manifest.minSdk;
        this.appInfo.targetSdkVersion = manifest.targetSdk;

        this.packageManager = new PackageManager(manifest, appInfo);
        this.contentResolver = new ContentResolver(this);
        this.windowManager = new HostWindowManager(new Display(resources.getDisplayMetrics()));
    }

    private static final class HostWindowManager implements WindowManager {
        private final Display defaultDisplay;

        HostWindowManager(Display display) {
            this.defaultDisplay = display;
        }

        @Override
        public Display getDefaultDisplay() {
            return defaultDisplay;
        }

        @Override
        public void addView(View view, ViewGroup.LayoutParams params) {
        }

        @Override
        public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        }

        @Override
        public void removeView(View view) {
        }
    }

    public void setApplicationContext(Context appCtx) {
        this.applicationContext = appCtx;
    }

    public void setActivityStarter(Consumer<Intent> starter) {
        this.activityStarter = starter;
    }

    public ApkFile getApk() {
        return apk;
    }

    public AndroidManifest getManifest() {
        return manifest;
    }

    @Override
    public AssetManager getAssets() {
        return resources.getAssets();
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public PackageManager getPackageManager() {
        return packageManager;
    }

    @Override
    public ContentResolver getContentResolver() {
        return contentResolver;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (receiver != null) {
            receivers.add(new RegisteredReceiver(receiver, filter));
        }
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        receivers.removeIf(r -> r.receiver == receiver);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        for (RegisteredReceiver r : receivers) {
            if (r.filter == null || (action != null && r.filter.hasAction(action))) {
                try {
                    r.receiver.onReceive(this, intent);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public Looper getMainLooper() {
        return Looper.getMainLooper();
    }

    @Override
    public Context getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setTheme(int resid) {
        theme.applyStyle(resid, true);
    }

    @Override
    public Resources.Theme getTheme() {
        return theme;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public String getPackageName() {
        return manifest.packageName;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return appInfo;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        String safe = (name == null || name.isEmpty()) ? "prefs" : name;
        return prefsCache.computeIfAbsent(safe,
                k -> new DiskSharedPreferences(dataDir.resolve("shared_prefs").resolve(k + ".properties")));
    }

    @Override
    public File getFilesDir() {
        return dataDir.resolve("files").toFile();
    }

    @Override
    public File getCacheDir() {
        return dataDir.resolve("cache").toFile();
    }

    @Override
    public File getExternalFilesDir(String type) {
        Path ext = dataDir.resolve("external").resolve(type != null ? type : "");
        try {
            Files.createDirectories(ext);
        } catch (Exception ignored) {
        }
        return ext.toFile();
    }

    @Override
    public void startActivity(Intent intent) {
        startActivity(intent, null);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        if (intent == null) return;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null && intent.getComponent() == null) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(intent.getData().toString()));
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        if (activityStarter != null) {
            activityStarter.accept(intent);
        }
    }

    @Override
    public Context createConfigurationContext(android.content.res.Configuration overrideConfiguration) {
        return this;
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            return new LayoutInflater(this);
        }
        if (WINDOW_SERVICE.equals(name)) {
            return windowManager;
        }
        return null;
    }
}
