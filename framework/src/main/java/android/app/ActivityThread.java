package android.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Lớp nội bộ android.app.ActivityThread hỗ trợ các ứng dụng/SDK dùng reflection truy vấn
 * currentActivityThread(), currentApplication(), currentPackageName() và hook mH.
 */
public final class ActivityThread {
    private static final ActivityThread INSTANCE = new ActivityThread();
    private static volatile Application currentApp;
    private static volatile String currentPkg = "dev.aaow.app";
    private static volatile String currentProc = "dev.aaow.app";

    public static final class H extends Handler {
        public static final int SCHEDULE_CRASH = 134;
        public static final int EXECUTE_TRANSACTION = 159;

        H() {
            super(Looper.getMainLooper());
        }
    }

    public final H mH = new H();

    private ActivityThread() {
    }

    public static void setPackageName(String pkg) {
        if (pkg != null && !pkg.isEmpty()) {
            currentPkg = pkg;
            currentProc = pkg;
        }
    }

    public static void setProcessName(String processName) {
        if (processName != null && !processName.isEmpty()) {
            currentProc = processName;
        }
    }

    public static void setCurrentApplication(Application app) {
        currentApp = app;
        if (app != null && app.getBaseContext() != null) {
            String pkg = app.getPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                setPackageName(pkg);
            }
        }
    }

    public static ActivityThread currentActivityThread() {
        return INSTANCE;
    }

    public static Application currentApplication() {
        return currentApp;
    }

    public static String currentPackageName() {
        if (currentApp != null && currentApp.getBaseContext() != null) {
            try {
                String pkg = currentApp.getPackageName();
                if (pkg != null && !pkg.isEmpty()) return pkg;
            } catch (Throwable ignored) {
            }
        }
        return currentPkg;
    }

    public static String currentProcessName() {
        String proc = currentProc;
        return (proc != null && !proc.isEmpty()) ? proc : currentPackageName();
    }

    public String getProcessName() {
        return currentProcessName();
    }

    public Application getApplication() {
        return currentApp;
    }

    public Context getSystemContext() {
        return currentApp;
    }
}
