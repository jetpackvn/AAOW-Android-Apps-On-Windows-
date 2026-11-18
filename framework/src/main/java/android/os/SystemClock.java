package android.os;

public final class SystemClock {
    private static final long BOOT_NANO = System.nanoTime();

    private SystemClock() {
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static long uptimeMillis() {
        return (System.nanoTime() - BOOT_NANO) / 1_000_000L;
    }

    public static long elapsedRealtime() {
        return uptimeMillis();
    }

    public static long elapsedRealtimeNanos() {
        return System.nanoTime() - BOOT_NANO;
    }
}
