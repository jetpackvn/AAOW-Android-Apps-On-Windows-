package android.os;

import java.util.concurrent.Executor;

/** Dịch vụ quản lý nguồn và WakeLock android.os.PowerManager. */
public final class PowerManager {
    public static final int PARTIAL_WAKE_LOCK = 0x00000001;
    public static final int SCREEN_DIM_WAKE_LOCK = 0x00000006;
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 0x0000000a;
    public static final int FULL_WAKE_LOCK = 0x0000001a;
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 0x00000020;
    public static final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;
    public static final int ON_AFTER_RELEASE = 0x20000000;

    public interface WakeLockStateListener {
        void onStateChanged(boolean enabled);
    }

    public final class WakeLock {
        private final int flags;
        private final String tag;
        private boolean referenceCounted = true;
        private int count;
        private WakeLockStateListener listener;
        private Executor listenerExecutor;

        WakeLock(int flags, String tag) {
            this.flags = flags;
            this.tag = tag;
        }

        public synchronized void setReferenceCounted(boolean value) {
            this.referenceCounted = value;
        }

        public synchronized void acquire() {
            acquire(-1);
        }

        public synchronized void acquire(long timeout) {
            boolean wasHeld = isHeld();
            if (referenceCounted) {
                count++;
            } else {
                count = 1;
            }
            if (!wasHeld && listener != null) {
                notifyListener(true);
            }
        }

        public synchronized void release() {
            release(0);
        }

        public synchronized void release(int flags) {
            if (referenceCounted) {
                if (count > 0) count--;
            } else {
                count = 0;
            }
            if (count == 0 && listener != null) {
                notifyListener(false);
            }
        }

        public synchronized boolean isHeld() {
            return count > 0;
        }

        public synchronized void setStateListener(Executor executor, WakeLockStateListener listener) {
            this.listenerExecutor = executor;
            this.listener = listener;
        }

        public void setWorkSource(Object ws) {
        }

        private void notifyListener(boolean enabled) {
            WakeLockStateListener l = this.listener;
            Executor ex = this.listenerExecutor;
            if (l != null) {
                if (ex != null) ex.execute(() -> l.onStateChanged(enabled));
                else l.onStateChanged(enabled);
            }
        }

        @Override
        public String toString() {
            return "WakeLock{" + tag + ", flags=0x" + Integer.toHexString(flags) + ", held=" + isHeld() + "}";
        }
    }

    public WakeLock newWakeLock(int levelAndFlags, String tag) {
        return new WakeLock(levelAndFlags, tag != null ? tag : "AAOW");
    }

    public boolean isInteractive() {
        return true;
    }

    @Deprecated
    public boolean isScreenOn() {
        return true;
    }

    public boolean isPowerSaveMode() {
        return false;
    }

    public boolean isDeviceIdleMode() {
        return false;
    }

    public boolean isLightDeviceIdleMode() {
        return false;
    }

    public boolean isIgnoringBatteryOptimizations(String packageName) {
        return true;
    }
}
