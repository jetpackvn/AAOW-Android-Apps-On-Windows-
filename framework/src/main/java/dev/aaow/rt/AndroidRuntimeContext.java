package dev.aaow.rt;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.print.PrintManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.microg.MicroGServiceRegistry;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Cài đặt gốc của android.content.Context cho một APK đang chạy trên Windows.
 * Quản lý lưu trữ thật (files, cache, databases, shared_prefs),
 * ContentResolver,
 * BroadcastReceiver, vòng đời Service nội bộ và dịch vụ hệ thống (Connectivity,
 * Window, Sensor...).
 */
public final class AndroidRuntimeContext extends Context {
    private static final String TAG = "AAOW-Context";

    private final ApkFile apk;
    private final AndroidManifest manifest;
    private final Resources resources;
    private final Resources.Theme theme;
    private final ApplicationInfo appInfo;
    private final PackageManager packageManager;
    private final ContentResolver contentResolver;
    private final WindowManager windowManager;
    private final ConnectivityManager connectivityManager;
    private final ClipboardManager clipboardManager;
    private final NotificationManager notificationManager;
    private final AudioManager audioManager;
    private final PrintManager printManager;
    private final PowerManager powerManager;
    private final ActivityManager activityManager;
    private final AlarmManager alarmManager;
    private final JobScheduler jobScheduler;
    private final android.os.UserManager userManager;
    private final ClassLoader classLoader;
    private final Path dataDir;
    private final Map<String, SharedPreferences> prefsCache = new ConcurrentHashMap<>();
    private final List<RegisteredReceiver> receivers = new CopyOnWriteArrayList<>();
    private final Map<String, Intent> stickyBroadcasts = new ConcurrentHashMap<>();
    private final Map<String, ServiceRecord> runningServices = new ConcurrentHashMap<>();
    private final Map<ServiceConnection, ServiceBinding> boundConnections = new ConcurrentHashMap<>();
    private final AtomicInteger nextStartId = new AtomicInteger(1);

    private Context applicationContext = this;
    private Application applicationInstance;
    private Consumer<Intent> activityStarter;
    private ActivityForResultStarter activityForResultStarter;
    private LayoutInflater cachedLayoutInflater;
    private final android.hardware.SensorManager sensorManager = new android.hardware.SensorManager();

    @FunctionalInterface
    public interface ActivityForResultStarter {
        void startActivityForResult(android.app.Activity caller, Intent intent, int requestCode);
    }

    private record RegisteredReceiver(BroadcastReceiver receiver, IntentFilter filter, Handler scheduler) {
    }

    private static final class ServiceRecord {
        final ComponentName component;
        final Service service;
        final Set<ServiceConnection> connections = new CopyOnWriteArraySet<>();
        boolean started;
        int lastStartId = -1;
        boolean bound;
        IBinder binder;
        Intent bindIntent;

        ServiceRecord(ComponentName component, Service service) {
            this.component = component;
            this.service = service;
        }
    }

    private record ServiceBinding(ServiceRecord record, Executor executor, ServiceConnection conn) {
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
            Files.createDirectories(dataDir.resolve("databases"));
        } catch (Exception ignored) {
        }

        Path nativeDir = NativeLibraryManager.prepareForApk(apk, dataDir);

        this.appInfo = new ApplicationInfo();
        this.appInfo.packageName = manifest.packageName;
        this.appInfo.processName = manifest.packageName;
        this.appInfo.name = manifest.applicationClass;
        this.appInfo.sourceDir = apk.getPath().toAbsolutePath().toString();
        this.appInfo.publicSourceDir = this.appInfo.sourceDir;
        this.appInfo.dataDir = dataDir.toAbsolutePath().toString();
        this.appInfo.nativeLibraryDir = nativeDir != null ? nativeDir.toAbsolutePath().toString()
                : dataDir.resolve("lib").toAbsolutePath().toString();
        this.appInfo.icon = manifest.iconRes;
        this.appInfo.theme = manifest.themeRes;
        this.appInfo.nonLocalizedLabel = manifest.label;
        this.appInfo.minSdkVersion = manifest.minSdk;
        this.appInfo.targetSdkVersion = manifest.targetSdk;

        this.packageManager = new PackageManager(manifest, appInfo, resources);
        this.contentResolver = new ContentResolver(this);
        this.contentResolver.registerProvider("com.google.android.gsf.gservices",
                new MicroGServiceRegistry.GServicesProvider());
        this.windowManager = new HostWindowManager(new Display(resources.getDisplayMetrics()));
        this.connectivityManager = new ConnectivityManager();
        this.clipboardManager = new ClipboardManager(this);
        this.notificationManager = new NotificationManager(this);
        this.audioManager = new AudioManager();
        this.printManager = new PrintManager();
        this.powerManager = new PowerManager();
        this.activityManager = new ActivityManager(this);
        this.alarmManager = new AlarmManager();
        this.jobScheduler = JobScheduler.createDefault();
        this.userManager = new android.os.UserManager();
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
        if (appCtx instanceof Application app) {
            this.applicationInstance = app;
        }
    }

    /**
     * Khởi tạo toàn bộ ContentProvider khai báo trong AndroidManifest.xml (theo thứ
     * tự initOrder giảm dần),
     * tương đương bước ActivityThread.installContentProviders() ngay trước
     * Application.onCreate().
     * Điều này giúp FirebaseInitProvider, AndroidX Startup
     * InitializationProvider... tự chạy đúng chuẩn Android.
     */
    public void installManifestContentProviders() {
        if (manifest == null || manifest.providers.isEmpty())
            return;
        List<AndroidManifest.Provider> ordered = new ArrayList<>();
        for (AndroidManifest.Provider p : manifest.providers) {
            if (p.enabled && p.name != null && !p.name.isEmpty()) {
                ordered.add(p);
            }
        }
        ordered.sort(Comparator.comparingInt((AndroidManifest.Provider p) -> p.initOrder).reversed());
        Context providerCtx = applicationInstance != null ? applicationInstance : this;

        for (AndroidManifest.Provider p : ordered) {
            if (p.authorities == null || p.authorities.isEmpty())
                continue;
            try {
                Class<?> cls = Class.forName(p.name, true, classLoader);
                if (!ContentProvider.class.isAssignableFrom(cls))
                    continue;
                ContentProvider cp = (ContentProvider) cls.getDeclaredConstructor().newInstance();
                ProviderInfo pi = packageManager.resolveContentProvider(p.authorities.split(";")[0].trim(),
                        PackageManager.GET_META_DATA);
                if (pi == null) {
                    pi = new ProviderInfo();
                    pi.packageName = manifest.packageName;
                    pi.name = p.name;
                    pi.authority = p.authorities;
                    pi.enabled = p.enabled;
                    pi.exported = p.exported;
                    pi.grantUriPermissions = p.grantUriPermissions;
                    pi.initOrder = p.initOrder;
                    pi.applicationInfo = appInfo;
                }
                cp.attachInfo(providerCtx, pi);
                for (String auth : p.authorities.split(";")) {
                    String clean = auth.trim();
                    if (!clean.isEmpty()) {
                        contentResolver.registerInitializedProvider(clean, cp);
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Bỏ qua ContentProvider khởi động " + p.name + ": " + t);
            }
        }
    }

    public void setActivityStarter(Consumer<Intent> starter) {
        this.activityStarter = starter;
    }

    public void setActivityForResultStarter(ActivityForResultStarter starter) {
        this.activityForResultStarter = starter;
    }

    public void dispatchStartActivityForResult(android.app.Activity caller, Intent intent, int requestCode) {
        if (activityForResultStarter != null) {
            activityForResultStarter.startActivityForResult(caller, intent, requestCode);
        } else if (activityStarter != null) {
            activityStarter.accept(intent);
        }
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

    // --------------------------------------------------------- BroadcastReceiver

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return registerReceiver(receiver, filter, null, null, 0);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return registerReceiver(receiver, filter, null, null, flags);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler) {
        return registerReceiver(receiver, filter, broadcastPermission, scheduler, 0);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler, int flags) {
        if (receiver != null) {
            receivers.add(new RegisteredReceiver(receiver, filter, scheduler));
        }
        if (filter != null) {
            for (Intent sticky : stickyBroadcasts.values()) {
                if (matchesFilter(filter, sticky)) {
                    return sticky;
                }
            }
        }
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        receivers.removeIf(r -> r.receiver == receiver);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        sendBroadcast(intent, null);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        if (intent == null)
            return;
        dispatchBroadcast(intent, null, null, 0, null, null);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        if (intent == null)
            return;
        dispatchBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        if (intent == null)
            return;
        String key = intent.getAction() != null ? intent.getAction() : intent.toString();
        stickyBroadcasts.put(key, new Intent(intent));
        sendBroadcast(intent);
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        if (intent == null)
            return;
        String key = intent.getAction() != null ? intent.getAction() : intent.toString();
        stickyBroadcasts.remove(key);
    }

    private void dispatchBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler resultScheduler,
            int initialCode, String initialData, Bundle initialExtras) {
        ComponentName targetComp = intent.getComponent();
        for (RegisteredReceiver r : receivers) {
            if (targetComp == null && !matchesFilter(r.filter, intent)) {
                continue;
            }
            if (targetComp != null && !r.receiver.getClass().getName().equals(targetComp.getClassName())) {
                continue;
            }
            Runnable deliver = () -> {
                try {
                    r.receiver.onReceive(this, intent);
                } catch (Throwable t) {
                    Log.w(TAG, "Lỗi trong BroadcastReceiver.onReceive(" + r.receiver.getClass().getName() + "): " + t);
                }
            };
            if (r.scheduler != null) {
                r.scheduler.post(deliver);
            } else {
                deliver.run();
            }
        }

        // Kiểm tra thêm các <receiver> tĩnh khai báo trong AndroidManifest.xml
        if (manifest != null) {
            for (AndroidManifest.Receiver mr : manifest.receivers) {
                if (!mr.enabled || mr.name == null)
                    continue;
                boolean matched = targetComp != null
                        ? mr.name.equals(targetComp.getClassName())
                        : matchesManifestFilters(mr.intentFilters, intent);
                if (matched) {
                    try {
                        Class<?> cls = Class.forName(mr.name, true, classLoader);
                        if (BroadcastReceiver.class.isAssignableFrom(cls)) {
                            BroadcastReceiver br = (BroadcastReceiver) cls.getDeclaredConstructor().newInstance();
                            br.onReceive(this, intent);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "Không gọi được manifest receiver " + mr.name + ": " + t);
                    }
                }
            }
        }

        if (resultReceiver != null) {
            Runnable deliverResult = () -> {
                resultReceiver.setResult(initialCode, initialData, initialExtras);
                try {
                    resultReceiver.onReceive(this, intent);
                } catch (Throwable ignored) {
                }
            };
            if (resultScheduler != null) {
                resultScheduler.post(deliverResult);
            } else {
                deliverResult.run();
            }
        }
    }

    private static boolean matchesManifestFilters(List<AndroidManifest.IntentFilterInfo> filters, Intent intent) {
        if (filters == null || filters.isEmpty())
            return false;
        String action = intent.getAction();
        for (AndroidManifest.IntentFilterInfo f : filters) {
            if (action != null && f.actions.contains(action)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesFilter(IntentFilter filter, Intent intent) {
        if (filter == null)
            return true;
        if (filter.countActions() > 0) {
            String action = intent.getAction();
            if (action == null || !filter.hasAction(action))
                return false;
        }
        if (!intent.getCategories().isEmpty() && filter.countCategories() > 0) {
            for (String cat : intent.getCategories()) {
                if (!filter.hasCategory(cat))
                    return false;
            }
        }
        if (filter.countDataSchemes() > 0) {
            String scheme = intent.getData() != null ? intent.getData().getScheme() : null;
            if (scheme == null || !filter.hasDataScheme(scheme))
                return false;
        }
        if (filter.countDataTypes() > 0) {
            String type = intent.getType();
            if (type == null || !filter.hasDataType(type))
                return false;
        }
        return true;
    }

    // --------------------------------------------------------- Service Lifecycle

    private ComponentName resolveServiceComponent(Intent service) {
        if (service == null)
            return null;
        ComponentName cmp = service.getComponent();
        if (cmp != null && cmp.getClassName() != null && !cmp.getClassName().isEmpty()) {
            String pkg = cmp.getPackageName() != null && !cmp.getPackageName().isEmpty()
                    ? cmp.getPackageName()
                    : manifest.packageName;
            String cls = cmp.getClassName();
            if (cls.startsWith("."))
                cls = pkg + cls;
            return new ComponentName(pkg, cls);
        }
        if (service.getAction() != null && manifest != null) {
            AndroidManifest.Service s = manifest.findServiceByAction(service.getAction());
            if (s != null && s.name != null) {
                return new ComponentName(manifest.packageName, s.name);
            }
        }
        return null;
    }

    private ServiceRecord getOrCreateService(ComponentName cmp) {
        if (cmp == null)
            return null;
        return runningServices.computeIfAbsent(cmp.getClassName(), clsName -> {
            try {
                Class<?> cls = Class.forName(clsName, true, classLoader);
                if (!Service.class.isAssignableFrom(cls))
                    return null;
                Service svc = (Service) cls.getDeclaredConstructor().newInstance();
                svc.attach(this, applicationInstance, clsName, startId -> stopSelfInternal(clsName, startId));
                svc.onCreate();
                return new ServiceRecord(cmp, svc);
            } catch (Throwable t) {
                Log.w(TAG, "Không khởi tạo được Service " + clsName + ": " + t);
                return null;
            }
        });
    }

    @Override
    public ComponentName startService(Intent service) {
        ComponentName cmp = resolveServiceComponent(service);
        if (cmp == null)
            return null;
        ServiceRecord rec = getOrCreateService(cmp);
        if (rec == null)
            return null;
        int startId = nextStartId.getAndIncrement();
        synchronized (rec) {
            rec.started = true;
            rec.lastStartId = startId;
        }
        try {
            rec.service.onStartCommand(service, 0, startId);
        } catch (Throwable t) {
            Log.w(TAG, "Lỗi trong Service.onStartCommand(" + cmp.getClassName() + "): " + t);
        }
        return cmp;
    }

    @Override
    public boolean stopService(Intent service) {
        ComponentName cmp = resolveServiceComponent(service);
        if (cmp == null)
            return false;
        return stopSelfInternal(cmp.getClassName(), -1);
    }

    private boolean stopSelfInternal(String className, int startId) {
        ServiceRecord rec = runningServices.get(className);
        if (rec == null)
            return false;
        boolean shouldDestroy;
        synchronized (rec) {
            if (startId >= 0 && startId != rec.lastStartId) {
                return false;
            }
            rec.started = false;
            shouldDestroy = rec.connections.isEmpty();
        }
        if (shouldDestroy && runningServices.remove(className, rec)) {
            try {
                rec.service.onDestroy();
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return bindService(service, flags, getMainExecutor(), conn);
    }

    @Override
    public boolean bindService(Intent service, int flags, Executor executor, ServiceConnection conn) {
        if (service == null || conn == null)
            return false;
        Executor exec = executor != null ? executor : getMainExecutor();
        if (MicroGServiceRegistry.canHandleService(service)) {
            return MicroGServiceRegistry.bindVirtualService(service, exec, conn);
        }
        ComponentName cmp = resolveServiceComponent(service);
        if (cmp == null)
            return false;

        ServiceRecord rec = (flags & BIND_AUTO_CREATE) != 0
                ? getOrCreateService(cmp)
                : runningServices.get(cmp.getClassName());
        if (rec == null)
            return false;

        final IBinder binder;
        synchronized (rec) {
            rec.connections.add(conn);
            rec.bindIntent = service;
            if (!rec.bound) {
                rec.bound = true;
                try {
                    rec.binder = rec.service.onBind(service);
                } catch (Throwable t) {
                    Log.w(TAG, "Lỗi trong Service.onBind(" + cmp.getClassName() + "): " + t);
                    rec.binder = null;
                }
            }
            binder = rec.binder;
        }
        boundConnections.put(conn, new ServiceBinding(rec, exec, conn));
        exec.execute(() -> {
            if (binder != null) {
                conn.onServiceConnected(cmp, binder);
            } else {
                conn.onNullBinding(cmp);
            }
        });
        return true;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        if (conn == null)
            return;
        ServiceBinding binding = boundConnections.remove(conn);
        if (binding == null)
            return;
        ServiceRecord rec = binding.record();
        boolean destroy = false;
        synchronized (rec) {
            rec.connections.remove(conn);
            if (rec.connections.isEmpty() && rec.bound) {
                rec.bound = false;
                try {
                    rec.service.onUnbind(rec.bindIntent);
                } catch (Throwable ignored) {
                }
                destroy = !rec.started;
            }
        }
        if (destroy && runningServices.remove(rec.component.getClassName(), rec)) {
            try {
                rec.service.onDestroy();
            } catch (Throwable ignored) {
            }
        }
    }

    public void stopAllServices() {
        for (var entry : new LinkedHashMap<>(runningServices).entrySet()) {
            ServiceRecord rec = entry.getValue();
            runningServices.remove(entry.getKey(), rec);
            try {
                if (rec.bound) {
                    rec.service.onUnbind(rec.bindIntent);
                }
                rec.service.onDestroy();
            } catch (Throwable ignored) {
            }
        }
        boundConnections.clear();
    }

    // --------------------------------------------------------- Context Metadata &
    // Storage

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

    private static String sanitizePrefsFileName(String name) {
        if (name == null || name.isEmpty())
            return "prefs";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        String safe = sanitizePrefsFileName(name);
        return prefsCache.computeIfAbsent(safe,
                k -> new DiskSharedPreferences(dataDir.resolve("shared_prefs").resolve(k + ".properties")));
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        String safe = sanitizePrefsFileName(name);
        prefsCache.remove(safe);
        try {
            return Files.deleteIfExists(dataDir.resolve("shared_prefs").resolve(safe + ".properties"));
        } catch (Exception e) {
            return false;
        }
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
    public File getExternalCacheDir() {
        Path ext = dataDir.resolve("external-cache");
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
        if (intent == null)
            return;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && intent.getComponent() == null) {
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
    public android.view.Display getDisplay() {
        return windowManager.getDefaultDisplay();
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (cachedLayoutInflater == null) {
                cachedLayoutInflater = new LayoutInflater(this);
            }
            return cachedLayoutInflater;
        }
        if (WINDOW_SERVICE.equals(name)) {
            return windowManager;
        }
        if (CONNECTIVITY_SERVICE.equals(name)) {
            return connectivityManager;
        }
        if (SENSOR_SERVICE.equals(name)) {
            return sensorManager;
        }
        if (ACCESSIBILITY_SERVICE.equals(name)) {
            return android.view.accessibility.AccessibilityManager.getInstance();
        }
        if (CLIPBOARD_SERVICE.equals(name)) {
            return clipboardManager;
        }
        if (NOTIFICATION_SERVICE.equals(name)) {
            return notificationManager;
        }
        if (AUDIO_SERVICE.equals(name)) {
            return audioManager;
        }
        if (PRINT_SERVICE.equals(name)) {
            return printManager;
        }
        if (INPUT_METHOD_SERVICE.equals(name)) {
            return InputMethodManager.getInstance();
        }
        if (POWER_SERVICE.equals(name)) {
            return powerManager;
        }
        if (ACTIVITY_SERVICE.equals(name)) {
            return activityManager;
        }
        if (ALARM_SERVICE.equals(name)) {
            return alarmManager;
        }
        if (JOB_SCHEDULER_SERVICE.equals(name)) {
            return jobScheduler;
        }
        if (USER_SERVICE.equals(name)) {
            return userManager;
        }
        return null;
    }
}
