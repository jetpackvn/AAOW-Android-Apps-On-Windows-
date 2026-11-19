package android.view;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Điều phối nhịp khung hình VSync (~60fps = 16ms) trên {@link Looper} hiện tại
 * (gắn vào Swing EDT khi ở Main Looper) cho AndroidX Animation, Kotlin Coroutines và RecyclerView.
 */
public final class Choreographer {
    private static final long FRAME_INTERVAL_MS = 16L;
    private static final long FRAME_INTERVAL_NS = 16_666_667L;
    private static final AtomicLong VSYNC_ID_SEQ = new AtomicLong(1L);

    private static final ThreadLocal<Choreographer> THREAD_INSTANCE = ThreadLocal.withInitial(() -> {
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        return new Choreographer(looper);
    });

    public interface FrameCallback {
        void doFrame(long frameTimeNanos);
    }

    public interface VsyncCallback {
        void onVsync(FrameData data);
    }

    public static class FrameTimeline {
        private final long vsyncId;
        private final long expectedPresentationTimeNanos;
        private final long deadlineNanos;

        FrameTimeline() {
            this(VSYNC_ID_SEQ.getAndIncrement(), System.nanoTime() + FRAME_INTERVAL_NS,
                    System.nanoTime() + FRAME_INTERVAL_NS);
        }

        FrameTimeline(long vsyncId, long expectedPresentationTimeNanos, long deadlineNanos) {
            this.vsyncId = vsyncId;
            this.expectedPresentationTimeNanos = expectedPresentationTimeNanos;
            this.deadlineNanos = deadlineNanos;
        }

        public long getVsyncId() {
            return vsyncId;
        }

        public long getExpectedPresentationTimeNanos() {
            return expectedPresentationTimeNanos;
        }

        public long getDeadlineNanos() {
            return deadlineNanos;
        }
    }

    public static class FrameData {
        private final long frameTimeNanos;
        private final FrameTimeline[] timelines;

        FrameData() {
            this(System.nanoTime());
        }

        FrameData(long frameTimeNanos) {
            this.frameTimeNanos = frameTimeNanos;
            long id = VSYNC_ID_SEQ.getAndIncrement();
            this.timelines = new FrameTimeline[]{
                    new FrameTimeline(id, frameTimeNanos + FRAME_INTERVAL_NS, frameTimeNanos + FRAME_INTERVAL_NS)
            };
        }

        public long getFrameTimeNanos() {
            return frameTimeNanos;
        }

        public FrameTimeline[] getFrameTimelines() {
            return timelines.clone();
        }

        public FrameTimeline getPreferredFrameTimeline() {
            return timelines[0];
        }
    }

    private record PendingEntry(long dueUptimeMs, FrameCallback frameCallback, VsyncCallback vsyncCallback) {
    }

    private final Handler handler;
    private final Object lock = new Object();
    private final List<PendingEntry> pending = new ArrayList<>();
    private boolean frameScheduled = false;
    private long lastFrameTimeNanos = 0L;

    private final Runnable frameRunner = this::doFrameDispatch;

    Choreographer() {
        this(Looper.getMainLooper());
    }

    private Choreographer(Looper looper) {
        this.handler = new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    public static Choreographer getInstance() {
        return THREAD_INSTANCE.get();
    }

    public void postFrameCallback(FrameCallback callback) {
        postFrameCallbackDelayed(callback, 0L);
    }

    public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        long due = SystemClock.uptimeMillis() + Math.max(0L, delayMillis);
        synchronized (lock) {
            pending.add(new PendingEntry(due, callback, null));
            scheduleNextLocked(Math.max(0L, delayMillis));
        }
    }

    public void removeFrameCallback(FrameCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (lock) {
            pending.removeIf(e -> e.frameCallback() == callback);
        }
    }

    public void postVsyncCallback(VsyncCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        long due = SystemClock.uptimeMillis();
        synchronized (lock) {
            pending.add(new PendingEntry(due, null, callback));
            scheduleNextLocked(0L);
        }
    }

    public void removeVsyncCallback(VsyncCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (lock) {
            pending.removeIf(e -> e.vsyncCallback() == callback);
        }
    }

    private void scheduleNextLocked(long requestedDelayMs) {
        if (frameScheduled && requestedDelayMs >= FRAME_INTERVAL_MS) {
            return;
        }
        long delay = Math.max(requestedDelayMs, FRAME_INTERVAL_MS);
        if (!frameScheduled) {
            frameScheduled = true;
            handler.postDelayed(frameRunner, delay);
        } else if (requestedDelayMs < FRAME_INTERVAL_MS) {
            handler.removeCallbacks(frameRunner);
            handler.postDelayed(frameRunner, FRAME_INTERVAL_MS);
        }
    }

    private void doFrameDispatch() {
        long nowMs = SystemClock.uptimeMillis();
        long frameTimeNanos = Math.max(System.nanoTime(), lastFrameTimeNanos + 1L);
        lastFrameTimeNanos = frameTimeNanos;

        List<PendingEntry> ready = new ArrayList<>();
        long nextDelayMs = -1L;

        synchronized (lock) {
            frameScheduled = false;
            Iterator<PendingEntry> it = pending.iterator();
            while (it.hasNext()) {
                PendingEntry entry = it.next();
                if (entry.dueUptimeMs() <= nowMs + 1L) {
                    ready.add(entry);
                    it.remove();
                } else {
                    long rem = entry.dueUptimeMs() - nowMs;
                    if (nextDelayMs < 0L || rem < nextDelayMs) {
                        nextDelayMs = rem;
                    }
                }
            }
            if (nextDelayMs >= 0L) {
                frameScheduled = true;
                handler.postDelayed(frameRunner, Math.max(FRAME_INTERVAL_MS, nextDelayMs));
            }
        }

        if (ready.isEmpty()) {
            return;
        }

        FrameData frameData = null;
        for (PendingEntry entry : ready) {
            if (entry.frameCallback() != null) {
                entry.frameCallback().doFrame(frameTimeNanos);
            } else if (entry.vsyncCallback() != null) {
                if (frameData == null) {
                    frameData = new FrameData(frameTimeNanos);
                }
                entry.vsyncCallback().onVsync(frameData);
            }
        }
    }
}
