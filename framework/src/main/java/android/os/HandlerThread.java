package android.os;

/**
 * Cài đặt thật android.os.HandlerThread chạy vòng lặp Looper.loop() trên luồng nền riêng.
 */
public class HandlerThread extends Thread {
    private Looper looper;
    private volatile boolean started;
    private volatile boolean finished;

    public HandlerThread(String name) {
        super(name);
        setDaemon(true);
    }

    public HandlerThread(String name, int priority) {
        super(name);
        setDaemon(true);
    }

    @Override
    public synchronized void start() {
        started = true;
        super.start();
    }

    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            looper = Looper.myLooper();
            notifyAll();
        }
        try {
            onLooperPrepared();
            Looper.loop();
        } finally {
            synchronized (this) {
                finished = true;
                notifyAll();
            }
        }
    }

    public Looper getLooper() {
        if (!started) {
            return null;
        }
        synchronized (this) {
            while (started && !finished && looper == null) {
                try {
                    wait(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
            return looper;
        }
    }

    public boolean quit() {
        Looper l = getLooper();
        if (l != null) {
            l.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely() {
        Looper l = getLooper();
        if (l != null) {
            l.quitSafely();
            return true;
        }
        return false;
    }

    public int getThreadId() {
        return (int) threadId();
    }
}
