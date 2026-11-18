package android.os;

import java.util.concurrent.Executor;

/** Cài đặt an toàn cho android.os.StrictMode trên Windows. */
public final class StrictMode {
    public interface OnThreadViolationListener {
        void onThreadViolation(Object violation);
    }

    public interface OnVmViolationListener {
        void onVmViolation(Object violation);
    }

    public static final class ThreadPolicy {
        public static final ThreadPolicy LAX = new ThreadPolicy();

        ThreadPolicy() {
        }

        public static final class Builder {
            public Builder() {}
            public Builder(ThreadPolicy policy) {}
            public ThreadPolicy build() { return LAX; }
            public Builder detectAll() { return this; }
            public Builder detectCustomSlowCalls() { return this; }
            public Builder detectDiskReads() { return this; }
            public Builder detectDiskWrites() { return this; }
            public Builder detectExplicitGc() { return this; }
            public Builder detectNetwork() { return this; }
            public Builder detectResourceMismatches() { return this; }
            public Builder detectUnbufferedIo() { return this; }
            public Builder penaltyDeath() { return this; }
            public Builder penaltyDeathOnNetwork() { return this; }
            public Builder penaltyDialog() { return this; }
            public Builder penaltyDropBox() { return this; }
            public Builder penaltyFlashScreen() { return this; }
            public Builder penaltyListener(Executor executor, OnThreadViolationListener listener) { return this; }
            public Builder penaltyLog() { return this; }
            public Builder permitAll() { return this; }
            public Builder permitCustomSlowCalls() { return this; }
            public Builder permitDiskReads() { return this; }
            public Builder permitDiskWrites() { return this; }
            public Builder permitExplicitGc() { return this; }
            public Builder permitNetwork() { return this; }
            public Builder permitResourceMismatches() { return this; }
            public Builder permitUnbufferedIo() { return this; }
        }
    }

    public static final class VmPolicy {
        public static final VmPolicy LAX = new VmPolicy();

        VmPolicy() {
        }

        public static final class Builder {
            public Builder() {}
            public Builder(VmPolicy policy) {}
            public VmPolicy build() { return LAX; }
            public Builder detectActivityLeaks() { return this; }
            public Builder detectAll() { return this; }
            public Builder detectBlockedBackgroundActivityLaunch() { return this; }
            public Builder detectCleartextNetwork() { return this; }
            public Builder detectContentUriWithoutPermission() { return this; }
            public Builder detectCredentialProtectedWhileLocked() { return this; }
            public Builder detectFileUriExposure() { return this; }
            public Builder detectImplicitDirectBoot() { return this; }
            public Builder detectImplicitUriPermissionGrant() { return this; }
            public Builder detectIncorrectContextUse() { return this; }
            public Builder detectLeakedClosableObjects() { return this; }
            public Builder detectLeakedRegistrationObjects() { return this; }
            public Builder detectLeakedSqlLiteObjects() { return this; }
            public Builder detectNonSdkApiUsage() { return this; }
            public Builder detectUnsafeIntentLaunch() { return this; }
            public Builder detectUntaggedSockets() { return this; }
            public Builder ignoreBlockedBackgroundActivityLaunch() { return this; }
            public Builder penaltyDeath() { return this; }
            public Builder penaltyDeathOnCleartextNetwork() { return this; }
            public Builder penaltyDeathOnFileUriExposure() { return this; }
            public Builder penaltyDropBox() { return this; }
            public Builder penaltyListener(Executor executor, OnVmViolationListener listener) { return this; }
            public Builder penaltyLog() { return this; }
            public Builder permitImplicitUriPermissionGrant() { return this; }
            public Builder permitNonSdkApiUsage() { return this; }
            public Builder permitUnsafeIntentLaunch() { return this; }
            public Builder setClassInstanceLimit(Class<?> klass, int instanceLimit) { return this; }
        }
    }

    private static volatile ThreadPolicy threadPolicy = ThreadPolicy.LAX;
    private static volatile VmPolicy vmPolicy = VmPolicy.LAX;

    private StrictMode() {
    }

    public static void setThreadPolicy(ThreadPolicy policy) {
        if (policy != null) threadPolicy = policy;
    }

    public static ThreadPolicy getThreadPolicy() {
        return threadPolicy;
    }

    public static ThreadPolicy allowThreadDiskReads() {
        return threadPolicy;
    }

    public static ThreadPolicy allowThreadDiskWrites() {
        return threadPolicy;
    }

    public static void setVmPolicy(VmPolicy policy) {
        if (policy != null) vmPolicy = policy;
    }

    public static VmPolicy getVmPolicy() {
        return vmPolicy;
    }

    public static void noteSlowCall(String name) {
    }
}
