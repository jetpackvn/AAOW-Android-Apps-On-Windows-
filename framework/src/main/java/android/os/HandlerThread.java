package android.os;

/**
 * Cài đặt android.os.HandlerThread cho phép các ứng dụng khởi chạy luồng nền có Looper riêng.
 */
public class HandlerThread extends Thread {
    private Looper looper;
    private volatile boolean quitting;

    public HandlerThread(String name) {
        super(name);
        setDaemon(true);
    }

    public HandlerThread(String name, int priority) {
        super(name);
        setDaemon(true);
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
        onLooperPrepared();
        while (!quitting && !isInterrupted()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public Looper getLooper() {
        if (!isAlive()) {
            return Looper.getMainLooper();
        }
        synchronized (this) {
            long deadline = System.currentTimeMillis() + 500;
            while (isAlive() && looper == null && System.currentTimeMillis() < deadline) {
                try {
                    wait(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
        return looper != null ? looper : Looper.getMainLooper();
    }

    public boolean quit() {
        quitting = true;
        interrupt();
        return true;
    }

    public boolean quitSafely() {
        return quit();
    }

    public int getThreadId() {
        return (int) threadId();
    }
}
