package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;

import java.io.File;

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
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (base != null) base.unregisterReceiver(receiver);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        if (base != null) base.sendBroadcast(intent);
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return base != null ? base.createConfigurationContext(overrideConfiguration) : this;
    }

    @Override
    public Looper getMainLooper() {
        return base.getMainLooper();
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
    public File getFilesDir() {
        return base.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        return base.getCacheDir();
    }

    @Override
    public File getExternalFilesDir(String type) {
        return base.getExternalFilesDir(type);
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
    public Object getSystemService(String name) {
        return base.getSystemService(name);
    }
}
