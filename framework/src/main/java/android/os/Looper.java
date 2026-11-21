package android.os;

import javax.swing.SwingUtilities;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bộ điều phối vòng lặp thông điệp của Android:
 * - MAIN_LOOPER ánh xạ sang Swing Event Dispatch Thread (EDT).
 * - Các Looper nền (tạo bởi HandlerThread / Looper.prepare()) có hàng đợi ưu tiên theo thời gian thực.
 */
public final class Looper {
    private static final Looper MAIN_LOOPER = new Looper(Thread.currentThread(), true);
    private static final ThreadLocal<Looper> TLS = new ThreadLocal<>();
    private static final AtomicLong SEQ_GEN = new AtomicLong(1);
    private static volatile Thread mainEdtThread;

    static final class QueuedTask {
        final long whenUptimeMs;
        final long seq;
        final Runnable runnable;
        volatile boolean cancelled;

        QueuedTask(long whenUptimeMs, long seq, Runnable runnable) {
            this.whenUptimeMs = whenUptimeMs;
            this.seq = seq;
            this.runnable = runnable;
        }
    }

    private final Thread thread;
    private final boolean main;
    private final MessageQueue messageQueue;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition avail = lock.newCondition();
    private final PriorityQueue<QueuedTask> queue = new PriorityQueue<>(
            Comparator.comparingLong((QueuedTask t) -> t.whenUptimeMs)
                    .thenComparingLong(t -> t.seq));
    private boolean quitting;
    private boolean quitSafely;

    private Looper(Thread thread, boolean main) {
        this.thread = thread;
        this.main = main;
        this.messageQueue = new MessageQueue(main);
    }

    public static void prepare() {
        TLS.set(new Looper(Thread.currentThread(), false));
    }

    public static void prepareMainLooper() {
        TLS.set(MAIN_LOOPER);
    }

    public static Looper getMainLooper() {
        return MAIN_LOOPER;
    }

    public static Looper myLooper() {
        Looper l = TLS.get();
        if (l != null) return l;
        if (SwingUtilities.isEventDispatchThread()) {
            return MAIN_LOOPER;
        }
        return null;
    }

    public static MessageQueue myQueue() {
        Looper l = myLooper();
        return l != null ? l.messageQueue : MAIN_LOOPER.messageQueue;
    }

    public MessageQueue getQueue() {
        return messageQueue;
    }

    public static void loop() {
        Looper me = myLooper();
        if (me == null || me.main) {
            return;
        }
        while (true) {
            QueuedTask task = me.takeNextDueTask();
            if (task == null) {
                return;
            }
            if (!task.cancelled && task.runnable != null) {
                task.runnable.run();
            }
        }
    }

    boolean isMain() {
        return main;
    }

    QueuedTask enqueue(Runnable r, long whenUptimeMs, boolean front) {
        long seq = front ? -SEQ_GEN.getAndIncrement() : SEQ_GEN.getAndIncrement();
        long when = front ? 0L : whenUptimeMs;
        QueuedTask qt = new QueuedTask(when, seq, r);
        lock.lock();
        try {
            if (quitting) return null;
            queue.add(qt);
            avail.signalAll();
            return qt;
        } finally {
            lock.unlock();
        }
    }

    void removeTask(QueuedTask task) {
        if (task == null) return;
        task.cancelled = true;
        lock.lock();
        try {
            queue.remove(task);
        } finally {
            lock.unlock();
        }
    }

    private QueuedTask takeNextDueTask() {
        lock.lock();
        try {
            while (true) {
                if (quitting && !quitSafely) {
                    queue.clear();
                    return null;
                }
                while (!queue.isEmpty() && queue.peek().cancelled) {
                    queue.poll();
                }
                if (queue.isEmpty()) {
                    if (quitting) {
                        return null;
                    }
                    try {
                        avail.await();
                    } catch (InterruptedException e) {
                        if (quitting) return null;
                    }
                    continue;
                }
                QueuedTask head = queue.peek();
                long now = SystemClock.uptimeMillis();
                long waitMs = head.whenUptimeMs - now;
                if (waitMs <= 0) {
                    return queue.poll();
                }
                if (quitSafely) {
                    // Đã hết task đến hạn khi quitSafely()
                    queue.clear();
                    return null;
                }
                try {
                    avail.await(waitMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (quitting && !quitSafely) {
                        queue.clear();
                        return null;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void quit() {
        if (main) return;
        lock.lock();
        try {
            quitting = true;
            quitSafely = false;
            queue.clear();
            avail.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void quitSafely() {
        if (main) return;
        lock.lock();
        try {
            quitting = true;
            quitSafely = true;
            avail.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isCurrentThread() {
        if (main) {
            return SwingUtilities.isEventDispatchThread() || Thread.currentThread() == getThread();
        }
        return Thread.currentThread() == thread;
    }

    public Thread getThread() {
        if (main) {
            if (SwingUtilities.isEventDispatchThread()) {
                mainEdtThread = Thread.currentThread();
                return mainEdtThread;
            }
            Thread cached = mainEdtThread;
            if (cached == null || !cached.isAlive()) {
                try {
                    SwingUtilities.invokeAndWait(() -> mainEdtThread = Thread.currentThread());
                    cached = mainEdtThread;
                } catch (Throwable ignored) {
                }
            }
            return cached != null ? cached : thread;
        }
        return thread;
    }
}
