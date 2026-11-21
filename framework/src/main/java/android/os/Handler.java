package android.os;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler của Android chạy đúng trên Looper tương ứng:
 * - Nếu gắn với MAIN_LOOPER: điều phối về Swing Event Dispatch Thread (EDT).
 * - Nếu gắn với Looper của HandlerThread: thực thi tuần tự trên luồng nền của HandlerThread đó.
 */
public class Handler {
    public interface Callback {
        boolean handleMessage(Message msg);
    }

    private static final ScheduledExecutorService MAIN_TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AAOW-HandlerTimer");
        t.setDaemon(true);
        return t;
    });

    private final Looper looper;
    final Callback callback;
    Callback mCallback;
    private final List<ScheduledEntry> scheduled = new CopyOnWriteArrayList<>();

    private static final class ScheduledEntry {
        final Runnable runnable;
        final int what;
        final Object token;
        final boolean hasMessage;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        volatile ScheduledFuture<?> future;
        volatile Looper.QueuedTask bgTask;

        ScheduledEntry(Runnable runnable, int what, Object token, boolean hasMessage) {
            this.runnable = runnable;
            this.what = what;
            this.token = token;
            this.hasMessage = hasMessage;
        }
    }

    public Handler() {
        this(Looper.myLooper(), null);
    }

    public Handler(Callback callback) {
        this(Looper.myLooper(), callback);
    }

    public Handler(Looper looper) {
        this(looper, null);
    }

    public Handler(Looper looper, Callback callback) {
        this.looper = looper != null ? looper : Looper.getMainLooper();
        this.callback = callback;
        this.mCallback = callback;
    }

    public static Handler createAsync(Looper looper) {
        return new Handler(looper);
    }

    public static Handler createAsync(Looper looper, Callback callback) {
        return new Handler(looper, callback);
    }

    public void handleMessage(Message msg) {
    }

    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            msg.callback.run();
        } else {
            Callback cb = mCallback != null ? mCallback : callback;
            if (cb != null && cb.handleMessage(msg)) return;
            handleMessage(msg);
        }
    }

    public final Looper getLooper() {
        return looper;
    }

    public String getMessageName(Message message) {
        if (message == null) return "null";
        if (message.callback != null) return message.callback.getClass().getName();
        return "0x" + Integer.toHexString(message.what);
    }

    public final Message obtainMessage() {
        return Message.obtain(this);
    }

    public final Message obtainMessage(int what) {
        return Message.obtain(this, what);
    }

    public final Message obtainMessage(int what, Object obj) {
        return Message.obtain(this, what, obj);
    }

    public final Message obtainMessage(int what, int arg1, int arg2) {
        return Message.obtain(this, what, arg1, arg2);
    }

    public final Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return Message.obtain(this, what, arg1, arg2, obj);
    }

    public final boolean post(Runnable r) {
        return sendMessageDelayed(getPostMessage(r, null), 0);
    }

    public final boolean postAtFrontOfQueue(Runnable r) {
        return sendMessageAtFrontOfQueue(getPostMessage(r, null));
    }

    public final boolean postAtTime(Runnable r, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r, null), uptimeMillis);
    }

    public final boolean postAtTime(Runnable r, Object token, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r, token), uptimeMillis);
    }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, null), delayMillis);
    }

    public final boolean postDelayed(Runnable r, Object token, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, token), delayMillis);
    }

    private static Message getPostMessage(Runnable r, Object token) {
        Message m = Message.obtain();
        m.obj = token;
        m.callback = r;
        return m;
    }

    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what, 0);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        return sendMessageDelayed(Message.obtain(this, what), delayMillis);
    }

    public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        return sendMessageAtTime(Message.obtain(this, what), uptimeMillis);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        long safeDelay = Math.max(0L, delayMillis);
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + safeDelay);
    }

    public final boolean sendMessageAtFrontOfQueue(Message msg) {
        return enqueueMessage(msg, 0L, true);
    }

    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        return enqueueMessage(msg, uptimeMillis, false);
    }

    private boolean enqueueMessage(Message msg, long uptimeMillis, boolean front) {
        if (msg == null) return false;
        msg.target = this;
        msg.when = uptimeMillis;
        boolean hasMsg = msg.callback == null;
        ScheduledEntry entry = new ScheduledEntry(msg.callback, msg.what, msg.obj, hasMsg);
        scheduled.add(entry);

        Runnable exec = () -> {
            scheduled.remove(entry);
            if (!entry.cancelled.get()) {
                try {
                    dispatchMessage(msg);
                } catch (LinkageError le) {
                    android.util.Log.e("AAOW-Handler", "LinkageError trong Handler (" + getMessageName(msg) + "): " + le, le);
                    throw le;
                }
            }
        };

        if (looper.isMain()) {
            long delay = front ? 0L : Math.max(0L, uptimeMillis - SystemClock.uptimeMillis());
            if (delay <= 0L) {
                SwingUtilities.invokeLater(exec);
            } else {
                entry.future = MAIN_TIMER.schedule(() -> {
                    if (!entry.cancelled.get()) {
                        SwingUtilities.invokeLater(exec);
                    } else {
                        scheduled.remove(entry);
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
            return true;
        }

        Looper.QueuedTask qt = looper.enqueue(exec, uptimeMillis, front);
        if (qt == null) {
            scheduled.remove(entry);
            return false;
        }
        entry.bgTask = qt;
        return true;
    }

    private void cancelEntry(ScheduledEntry s) {
        s.cancelled.set(true);
        if (s.future != null) s.future.cancel(false);
        if (s.bgTask != null) looper.removeTask(s.bgTask);
        scheduled.remove(s);
    }

    public final void removeCallbacks(Runnable r) {
        if (r == null) return;
        for (ScheduledEntry s : scheduled) {
            if (s.runnable == r) {
                cancelEntry(s);
            }
        }
    }

    public final void removeCallbacks(Runnable r, Object token) {
        if (r == null) return;
        for (ScheduledEntry s : scheduled) {
            if (s.runnable == r && (token == null || Objects.equals(token, s.token))) {
                cancelEntry(s);
            }
        }
    }

    public final void removeMessages(int what) {
        for (ScheduledEntry s : scheduled) {
            if (s.hasMessage && s.what == what) {
                cancelEntry(s);
            }
        }
    }

    public final void removeMessages(int what, Object object) {
        for (ScheduledEntry s : scheduled) {
            if (s.hasMessage && s.what == what && (object == null || Objects.equals(object, s.token))) {
                cancelEntry(s);
            }
        }
    }

    public final void removeCallbacksAndMessages(Object token) {
        for (ScheduledEntry s : scheduled) {
            if (token == null || Objects.equals(token, s.token)) {
                cancelEntry(s);
            }
        }
    }

    public final boolean hasMessages(int what) {
        for (ScheduledEntry s : scheduled) {
            if (!s.cancelled.get() && s.hasMessage && s.what == what) return true;
        }
        return false;
    }

    public final boolean hasMessages(int what, Object object) {
        for (ScheduledEntry s : scheduled) {
            if (!s.cancelled.get() && s.hasMessage && s.what == what
                    && (object == null || Objects.equals(object, s.token))) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasCallbacks(Runnable r) {
        if (r == null) return false;
        for (ScheduledEntry s : scheduled) {
            if (!s.cancelled.get() && s.runnable == r) return true;
        }
        return false;
    }
}
