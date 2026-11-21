package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintManager;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;

public abstract class Context {
    public static final int MODE_PRIVATE = 0x0000;
    public static final int MODE_WORLD_READABLE = 0x0001;
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    public static final int MODE_MULTI_PROCESS = 0x0004;
    public static final int MODE_ENABLE_WRITE_AHEAD_LOGGING = 0x0008;
    public static final int MODE_NO_LOCALIZED_COLLATORS = 0x0010;
    public static final int MODE_APPEND = 0x8000;

    public static final int CONTEXT_INCLUDE_CODE = 0x00000001;
    public static final int CONTEXT_IGNORE_SECURITY = 0x00000002;
    public static final int CONTEXT_RESTRICTED = 0x00000004;

    public static final int BIND_AUTO_CREATE = 0x0001;
    public static final int BIND_DEBUG_UNBIND = 0x0002;
    public static final int BIND_NOT_FOREGROUND = 0x0004;
    public static final int BIND_ABOVE_CLIENT = 0x0008;
    public static final int BIND_ALLOW_OOM_MANAGEMENT = 0x0010;
    public static final int BIND_WAIVE_PRIORITY = 0x0020;
    public static final int BIND_IMPORTANT = 0x0040;
    public static final int BIND_ADJUST_WITH_ACTIVITY = 0x0080;

    public static final int RECEIVER_VISIBLE_TO_INSTANT_APPS = 0x1;
    public static final int RECEIVER_EXPORTED = 0x2;
    public static final int RECEIVER_NOT_EXPORTED = 0x4;

    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final String WINDOW_SERVICE = "window";
    public static final String CLIPBOARD_SERVICE = "clipboard";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String SENSOR_SERVICE = "sensor";
    public static final String ACCESSIBILITY_SERVICE = "accessibility";
    public static final String CONNECTIVITY_SERVICE = "connectivity";
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String ALARM_SERVICE = "alarm";
    public static final String AUDIO_SERVICE = "audio";
    public static final String POWER_SERVICE = "power";
    public static final String STORAGE_SERVICE = "storage";
    public static final String DOWNLOAD_SERVICE = "download";
    public static final String INPUT_METHOD_SERVICE = "input_method";
    public static final String JOB_SCHEDULER_SERVICE = "jobscheduler";
    public static final String PRINT_SERVICE = "print";
    public static final String USER_SERVICE = "user";
    public static final String WIFI_SERVICE = "wifi";
    public static final String TELEPHONY_SERVICE = "phone";

    public abstract AssetManager getAssets();

    public abstract Resources getResources();

    public abstract PackageManager getPackageManager();

    public abstract ContentResolver getContentResolver();

    public abstract Looper getMainLooper();

    public abstract Context getApplicationContext();

    public abstract void setTheme(int resid);

    public abstract Resources.Theme getTheme();

    public abstract ClassLoader getClassLoader();

    public abstract String getPackageName();

    public abstract ApplicationInfo getApplicationInfo();

    public abstract SharedPreferences getSharedPreferences(String name, int mode);

    public abstract File getFilesDir();

    public abstract File getCacheDir();

    public abstract File getExternalFilesDir(String type);

    public File[] getExternalFilesDirs(String type) {
        return new File[]{getExternalFilesDir(type)};
    }

    public File getExternalCacheDir() {
        return getCacheDir();
    }

    public File[] getExternalCacheDirs() {
        return new File[]{getExternalCacheDir()};
    }

    public File getCodeCacheDir() {
        return getCacheDir();
    }

    public File getNoBackupFilesDir() {
        return getFilesDir();
    }

    public File getObbDir() {
        return getExternalFilesDir("obb");
    }

    public File getDataDir() {
        File f = getFilesDir();
        return f != null && f.getParentFile() != null ? f.getParentFile() : f;
    }

    public File getDir(String name, int mode) {
        File dir = new File(getDataDir(), "app_" + name);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public File getFileStreamPath(String name) {
        return new File(getFilesDir(), name);
    }

    public String[] fileList() {
        File files = getFilesDir();
        String[] list = files != null ? files.list() : null;
        return list != null ? list : new String[0];
    }

    public boolean deleteFile(String name) {
        return new File(getFilesDir(), name).delete();
    }

    public boolean deleteSharedPreferences(String name) {
        return false;
    }

    public File getDatabasePath(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Database name must not be empty");
        }
        File f = new File(name);
        if (f.isAbsolute()) return f;
        File dbDir = new File(getDataDir(), "databases");
        if (!dbDir.exists()) dbDir.mkdirs();
        return new File(dbDir, name);
    }

    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return openOrCreateDatabase(name, mode, factory, null);
    }

    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                               DatabaseErrorHandler errorHandler) {
        int flags = SQLiteDatabase.CREATE_IF_NECESSARY;
        if ((mode & MODE_ENABLE_WRITE_AHEAD_LOGGING) != 0) {
            flags |= SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING;
        }
        if ((mode & MODE_NO_LOCALIZED_COLLATORS) != 0) {
            flags |= SQLiteDatabase.NO_LOCALIZED_COLLATORS;
        }
        return SQLiteDatabase.openDatabase(getDatabasePath(name).getAbsolutePath(), factory, flags, errorHandler);
    }

    public boolean deleteDatabase(String name) {
        return SQLiteDatabase.deleteDatabase(getDatabasePath(name));
    }

    public String[] databaseList() {
        File dbDir = new File(getDataDir(), "databases");
        String[] list = dbDir.list();
        return list != null ? list : new String[0];
    }

    public String getPackageCodePath() {
        ApplicationInfo info = getApplicationInfo();
        return info != null ? info.sourceDir : null;
    }

    public String getPackageResourcePath() {
        return getPackageCodePath();
    }

    public Executor getMainExecutor() {
        Handler h = new Handler(getMainLooper());
        return h::post;
    }

    public abstract void startActivity(Intent intent);

    public abstract void startActivity(Intent intent, Bundle options);

    public ComponentName startService(Intent service) {
        return null;
    }

    public ComponentName startForegroundService(Intent service) {
        return startService(service);
    }

    public boolean stopService(Intent service) {
        return false;
    }

    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return false;
    }

    public boolean bindService(Intent service, int flags, Executor executor, ServiceConnection conn) {
        return bindService(service, conn, flags);
    }

    public void unbindService(ServiceConnection conn) {
    }

    public abstract Object getSystemService(String name);

    public final <T> T getSystemService(Class<T> serviceClass) {
        String serviceName = getSystemServiceName(serviceClass);
        return serviceName != null ? serviceClass.cast(getSystemService(serviceName)) : null;
    }

    public String getSystemServiceName(Class<?> serviceClass) {
        if (serviceClass == null) return null;
        if (serviceClass == ConnectivityManager.class) return CONNECTIVITY_SERVICE;
        if (serviceClass == WindowManager.class) return WINDOW_SERVICE;
        if (serviceClass == LayoutInflater.class) return LAYOUT_INFLATER_SERVICE;
        if (serviceClass == SensorManager.class) return SENSOR_SERVICE;
        if (serviceClass == AccessibilityManager.class) return ACCESSIBILITY_SERVICE;
        if (serviceClass == ClipboardManager.class) return CLIPBOARD_SERVICE;
        if (serviceClass == android.app.NotificationManager.class) return NOTIFICATION_SERVICE;
        if (serviceClass == AudioManager.class) return AUDIO_SERVICE;
        if (serviceClass == PrintManager.class) return PRINT_SERVICE;
        if (serviceClass == InputMethodManager.class) return INPUT_METHOD_SERVICE;
        if (serviceClass == android.os.PowerManager.class) return POWER_SERVICE;
        if (serviceClass == android.app.ActivityManager.class) return ACTIVITY_SERVICE;
        if (serviceClass == android.app.AlarmManager.class) return ALARM_SERVICE;
        if (serviceClass == android.app.job.JobScheduler.class) return JOB_SCHEDULER_SERVICE;
        if (serviceClass == android.os.UserManager.class) return USER_SERVICE;
        if (serviceClass == android.net.wifi.WifiManager.class) return WIFI_SERVICE;
        if (serviceClass == android.telephony.TelephonyManager.class) return TELEPHONY_SERVICE;
        return null;
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
                                   String broadcastPermission, Handler scheduler) {
        return registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
                                   String broadcastPermission, Handler scheduler, int flags) {
        return registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
    }

    public void registerComponentCallbacks(ComponentCallbacks callback) {
        Context app = getApplicationContext();
        if (app != null && app != this) {
            app.registerComponentCallbacks(callback);
        }
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        Context app = getApplicationContext();
        if (app != null && app != this) {
            app.unregisterComponentCallbacks(callback);
        }
    }

    public void sendBroadcast(Intent intent) {
    }

    public void sendBroadcast(Intent intent, String receiverPermission) {
        sendBroadcast(intent);
    }

    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        sendBroadcast(intent, receiverPermission);
    }

    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
                                     Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendBroadcast(intent, receiverPermission);
    }

    public void sendStickyBroadcast(Intent intent) {
        sendBroadcast(intent);
    }

    public void removeStickyBroadcast(Intent intent) {
    }

    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return this;
    }

    public Context createDeviceProtectedStorageContext() {
        return this;
    }

    public Context createPackageContext(String packageName, int flags)
            throws PackageManager.NameNotFoundException {
        if (packageName == null || packageName.equals(getPackageName())
                || dev.aaow.rt.microg.MicroGServiceRegistry.isVirtualGmsPackage(packageName)) {
            return this;
        }
        throw new PackageManager.NameNotFoundException("Package not found: " + packageName);
    }

    public boolean isDeviceProtectedStorage() {
        return false;
    }

    public final CharSequence getText(int resId) {
        return getResources().getText(resId);
    }

    public final String getString(int resId) {
        return getResources().getString(resId);
    }

    public final String getString(int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    public final int getColor(int id) {
        return getResources().getColor(id, getTheme());
    }

    public final android.content.res.ColorStateList getColorStateList(int id) {
        return getResources().getColorStateList(id, getTheme());
    }

    public final Drawable getDrawable(int id) {
        return getResources().getDrawable(id, getTheme());
    }

    public int checkSelfPermission(String permission) {
        return PackageManager.PERMISSION_GRANTED;
    }

    public int checkPermission(String permission, int pid, int uid) {
        return PackageManager.PERMISSION_GRANTED;
    }

    public int checkCallingOrSelfPermission(String permission) {
        return PackageManager.PERMISSION_GRANTED;
    }

    public boolean isRestricted() {
        return false;
    }

    public Display getDisplay() {
        Object wm = getSystemService(WINDOW_SERVICE);
        if (wm instanceof WindowManager w) {
            return w.getDefaultDisplay();
        }
        return new Display(getResources() != null ? getResources().getDisplayMetrics() : null);
    }

    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return new FileInputStream(new File(getFilesDir(), name));
    }

    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(new File(getFilesDir(), name), (mode & MODE_APPEND) != 0);
    }

    public final TypedArray obtainStyledAttributes(int[] attrs) {
        return getTheme().obtainStyledAttributes(attrs);
    }

    public final TypedArray obtainStyledAttributes(int resid, int[] attrs) {
        return getTheme().obtainStyledAttributes(resid, attrs);
    }

    public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs) {
        return getTheme().obtainStyledAttributes(set, attrs, 0, 0);
    }

    public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
        return getTheme().obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
    }
}
