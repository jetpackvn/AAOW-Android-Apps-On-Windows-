package android.app;

import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Lớp cơ sở cho các thành phần chạy nền android.app.Service. */
public abstract class Service extends ContextWrapper implements ComponentCallbacks2 {
    public static final int START_CONTINUATION_MASK = 15;
    public static final int START_FLAG_REDELIVERY = 1;
    public static final int START_FLAG_RETRY = 2;
    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int START_STICKY = 1;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;

    public static final int STOP_FOREGROUND_LEGACY = 0;
    public static final int STOP_FOREGROUND_REMOVE = 1;
    public static final int STOP_FOREGROUND_DETACH = 2;

    private Application application;
    private String className;
    private StopSelfBridge stopSelfBridge;
    private int lastStartId = -1;

    @FunctionalInterface
    public interface StopSelfBridge {
        boolean stopServiceResult(int startId);
    }

    public Service() {
        super(null);
    }

    public final void attach(Context context, Application application, String className, StopSelfBridge stopSelfBridge) {
        attachBaseContext(context);
        this.application = application;
        this.className = className;
        this.stopSelfBridge = stopSelfBridge;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    public final Application getApplication() {
        return application;
    }

    public final ComponentName getComponentName() {
        return new ComponentName(getPackageName() != null ? getPackageName() : "", className != null ? className : getClass().getName());
    }

    public void onCreate() {
    }

    @Deprecated
    public void onStart(Intent intent, int startId) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        this.lastStartId = startId;
        onStart(intent, startId);
        return START_STICKY_COMPATIBILITY;
    }

    public abstract IBinder onBind(Intent intent);

    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void onRebind(Intent intent) {
    }

    public void onDestroy() {
    }

    public void onTaskRemoved(Intent rootIntent) {
    }

    public void onTimeout(int startId) {
    }

    public void onTimeout(int startId, int fgsType) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }

    public final void stopSelf() {
        stopSelf(-1);
    }

    public final void stopSelf(int startId) {
        stopSelfResult(startId);
    }

    public final boolean stopSelfResult(int startId) {
        if (stopSelfBridge != null) {
            return stopSelfBridge.stopServiceResult(startId);
        }
        return true;
    }

    public final void startForeground(int id, Notification notification) {
        startForeground(id, notification, 0);
    }

    public final void startForeground(int id, Notification notification, int foregroundServiceType) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && notification != null) {
                nm.notify(id, notification);
            }
        } catch (Throwable ignored) {
        }
    }

    public final void stopForeground(boolean removeNotification) {
        stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : STOP_FOREGROUND_DETACH);
    }

    public final void stopForeground(int flags) {
    }

    public final int getForegroundServiceType() {
        return 0;
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }
}
