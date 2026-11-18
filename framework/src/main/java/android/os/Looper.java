package android.os;

/**
 * Ánh xạ Looper của Android sang Event Dispatch Thread (EDT) của Windows/Swing.
 */
public final class Looper {
    private static final Looper MAIN_LOOPER = new Looper(Thread.currentThread());
    private static final ThreadLocal<Looper> TLS = ThreadLocal.withInitial(() -> MAIN_LOOPER);

    private final Thread thread;

    private Looper(Thread thread) {
        this.thread = thread;
    }

    public static void prepare() {
        TLS.set(new Looper(Thread.currentThread()));
    }

    public static void prepareMainLooper() {
    }

    public static Looper getMainLooper() {
        return MAIN_LOOPER;
    }

    public static Looper myLooper() {
        return TLS.get();
    }

    public static void loop() {
    }

    public boolean isCurrentThread() {
        return javax.swing.SwingUtilities.isEventDispatchThread() || Thread.currentThread() == thread;
    }

    public Thread getThread() {
        return thread;
    }
}
