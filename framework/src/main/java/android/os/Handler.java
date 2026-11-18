package android.os;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handler của Android chạy trên luồng giao diện Windows (Swing EDT),
 * hỗ trợ post, postDelayed, sendMessage, sendEmptyMessageDelayed, removeCallbacks.
 */
public class Handler {
    public interface Callback {
        boolean handleMessage(Message msg);
    }

    private final Looper looper;
    private final Callback callback;
    private final List<Scheduled> scheduled = new CopyOnWriteArrayList<>();

    private record Scheduled(Runnable runnable, int what, Timer timer) {
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
    }

    public void handleMessage(Message msg) {
    }

    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            msg.callback.run();
        } else {
            if (callback != null && callback.handleMessage(msg)) return;
            handleMessage(msg);
        }
    }

    public final Looper getLooper() {
        return looper;
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

    public final boolean post(Runnable r) {
        SwingUtilities.invokeLater(r);
        return true;
    }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        int delay = (int) Math.max(0, delayMillis);
        final Scheduled[] holder = new Scheduled[1];
        Timer t = new Timer(delay, e -> {
            if (holder[0] != null) scheduled.remove(holder[0]);
            r.run();
        });
        t.setRepeats(false);
        holder[0] = new Scheduled(r, -1, t);
        scheduled.add(holder[0]);
        t.start();
        return true;
    }

    public final void removeCallbacks(Runnable r) {
        for (Scheduled s : scheduled) {
            if (s.runnable == r) {
                s.timer.stop();
                scheduled.remove(s);
            }
        }
    }

    public final void removeMessages(int what) {
        for (Scheduled s : scheduled) {
            if (s.what == what) {
                s.timer.stop();
                scheduled.remove(s);
            }
        }
    }

    public final void removeCallbacksAndMessages(Object token) {
        for (Scheduled s : scheduled) {
            s.timer.stop();
        }
        scheduled.clear();
    }

    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what, 0);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        Message msg = Message.obtain(this, what);
        return sendMessageDelayed(msg, delayMillis);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        msg.target = this;
        if (delayMillis <= 0) {
            SwingUtilities.invokeLater(() -> dispatchMessage(msg));
            return true;
        }
        int delay = (int) delayMillis;
        final Scheduled[] holder = new Scheduled[1];
        Timer t = new Timer(delay, e -> {
            if (holder[0] != null) scheduled.remove(holder[0]);
            dispatchMessage(msg);
        });
        t.setRepeats(false);
        holder[0] = new Scheduled(msg.callback, msg.what, t);
        scheduled.add(holder[0]);
        t.start();
        return true;
    }
}
