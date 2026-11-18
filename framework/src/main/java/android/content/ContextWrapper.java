package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;

public class ContextWrapper extends Context {
    Context base;

    public ContextWrapper(Context base) {
        this.base = base;
    }

    protected void attachBaseContext(Context base) {
        this.base = base;
    }

    public Context getBaseContext() {
        return base;
    }

    @Override
    public AssetManager getAssets() {
        return base.getAssets();
    }

    @Override
    public Resources getResources() {
        return base.getResources();
    }

    @Override
    public PackageManager getPackageManager() {
        return base.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver() {
        return base.getContentResolver();
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return base != null ? base.registerReceiver(receiver, filter) : null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return base != null ? base.registerReceiver(receiver, filter, flags) : null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
                                   String broadcastPermission, Handler scheduler) {
        return base != null ? base.registerReceiver(receiver, filter, broadcastPermission, scheduler) : null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
                                   String broadcastPermission, Handler scheduler, int flags) {
        return base != null ? base.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags) : null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (base != null) base.unregisterReceiver(receiver);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        if (base != null) base.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        if (base != null) base.sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        if (base != null) base.sendOrderedBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
                                     Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        if (base != null) {
            base.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler,
                    initialCode, initialData, initialExtras);
        }
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        if (base != null) base.sendStickyBroadcast(intent);
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        if (base != null) base.removeStickyBroadcast(intent);
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return base != null ? base.createConfigurationContext(overrideConfiguration) : this;
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return base != null ? base.createDeviceProtectedStorageContext() : this;
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return base != null && base.isDeviceProtectedStorage();
    }

    @Override
    public Looper getMainLooper() {
        return base.getMainLooper();
    }

    @Override
    public Executor getMainExecutor() {
        return base != null ? base.getMainExecutor() : super.getMainExecutor();
    }

    @Override
    public Context getApplicationContext() {
        return base.getApplicationContext();
    }

    @Override
    public void setTheme(int resid) {
        if (base != null) base.setTheme(resid);
    }

    @Override
    public Resources.Theme getTheme() {
        return base.getTheme();
    }

    @Override
    public ClassLoader getClassLoader() {
        return base.getClassLoader();
    }

    @Override
    public String getPackageName() {
        return base.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return base.getApplicationInfo();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return base.getSharedPreferences(name, mode);
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        return base != null && base.deleteSharedPreferences(name);
    }

    @Override
    public File getFilesDir() {
        return base.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        return base.getCacheDir();
    }

    @Override
    public File getCodeCacheDir() {
        return base != null ? base.getCodeCacheDir() : super.getCodeCacheDir();
    }

    @Override
    public File getExternalFilesDir(String type) {
        return base.getExternalFilesDir(type);
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return base != null ? base.getExternalFilesDirs(type) : super.getExternalFilesDirs(type);
    }

    @Override
    public File getExternalCacheDir() {
        return base != null ? base.getExternalCacheDir() : super.getExternalCacheDir();
    }

    @Override
    public File[] getExternalCacheDirs() {
        return base != null ? base.getExternalCacheDirs() : super.getExternalCacheDirs();
    }

    @Override
    public File getNoBackupFilesDir() {
        return base != null ? base.getNoBackupFilesDir() : super.getNoBackupFilesDir();
    }

    @Override
    public File getDataDir() {
        return base != null ? base.getDataDir() : super.getDataDir();
    }

    @Override
    public File getDir(String name, int mode) {
        return base != null ? base.getDir(name, mode) : super.getDir(name, mode);
    }

    @Override
    public File getFileStreamPath(String name) {
        return base != null ? base.getFileStreamPath(name) : super.getFileStreamPath(name);
    }

    @Override
    public String[] fileList() {
        return base != null ? base.fileList() : super.fileList();
    }

    @Override
    public boolean deleteFile(String name) {
        return base != null ? base.deleteFile(name) : super.deleteFile(name);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return base != null ? base.openFileInput(name) : super.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return base != null ? base.openFileOutput(name, mode) : super.openFileOutput(name, mode);
    }

    @Override
    public File getDatabasePath(String name) {
        return base != null ? base.getDatabasePath(name) : super.getDatabasePath(name);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return base != null ? base.openOrCreateDatabase(name, mode, factory) : super.openOrCreateDatabase(name, mode, factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                               DatabaseErrorHandler errorHandler) {
        return base != null ? base.openOrCreateDatabase(name, mode, factory, errorHandler)
                : super.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return base != null ? base.deleteDatabase(name) : super.deleteDatabase(name);
    }

    @Override
    public String[] databaseList() {
        return base != null ? base.databaseList() : super.databaseList();
    }

    @Override
    public String getPackageCodePath() {
        return base != null ? base.getPackageCodePath() : super.getPackageCodePath();
    }

    @Override
    public String getPackageResourcePath() {
        return base != null ? base.getPackageResourcePath() : super.getPackageResourcePath();
    }

    @Override
    public void startActivity(Intent intent) {
        base.startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        base.startActivity(intent, options);
    }

    @Override
    public ComponentName startService(Intent service) {
        return base != null ? base.startService(service) : null;
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        return base != null ? base.startForegroundService(service) : null;
    }

    @Override
    public boolean stopService(Intent name) {
        return base != null && base.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return base != null && base.bindService(service, conn, flags);
    }

    @Override
    public boolean bindService(Intent service, int flags, Executor executor, ServiceConnection conn) {
        return base != null && base.bindService(service, flags, executor, conn);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        if (base != null) base.unbindService(conn);
    }

    @Override
    public Object getSystemService(String name) {
        return base.getSystemService(name);
    }

    @Override
    public String getSystemServiceName(Class<?> serviceClass) {
        return base != null ? base.getSystemServiceName(serviceClass) : super.getSystemServiceName(serviceClass);
    }

    @Override
    public boolean isRestricted() {
        return base != null && base.isRestricted();
    }

    @Override
    public android.view.Display getDisplay() {
        return base != null ? base.getDisplay() : super.getDisplay();
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return base != null ? base.checkPermission(permission, pid, uid) : super.checkPermission(permission, pid, uid);
    }

    @Override
    public int checkSelfPermission(String permission) {
        return base != null ? base.checkSelfPermission(permission) : super.checkSelfPermission(permission);
    }
}
