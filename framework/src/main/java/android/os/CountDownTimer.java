package android.os;

/**
 * Cài đặt android.os.CountDownTimer chạy trên luồng giao diện Windows qua Handler.
 */
public abstract class CountDownTimer {
    private final long millisInFuture;
    private final long countDownInterval;
    private long stopTimeInFuture;
    private boolean cancelled = false;
    private final Handler handler = new Handler();

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (CountDownTimer.this) {
                if (cancelled) return;
                final long millisLeft = stopTimeInFuture - SystemClock.elapsedRealtime();
                if (millisLeft <= 0) {
                    onFinish();
                } else {
                    long lastTickStart = SystemClock.elapsedRealtime();
                    onTick(millisLeft);
                    long lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart;
                    long delay;
                    if (millisLeft < countDownInterval) {
                        delay = millisLeft - lastTickDuration;
                        if (delay < 0) delay = 0;
                    } else {
                        delay = countDownInterval - lastTickDuration;
                        while (delay < 0) delay += countDownInterval;
                    }
                    handler.postDelayed(this, delay);
                }
            }
        }
    };

    public CountDownTimer(long millisInFuture, long countDownInterval) {
        this.millisInFuture = millisInFuture;
        this.countDownInterval = Math.max(1L, countDownInterval);
    }

    public final synchronized void cancel() {
        cancelled = true;
        handler.removeCallbacks(tickRunnable);
    }

    public final synchronized CountDownTimer start() {
        cancelled = false;
        if (millisInFuture <= 0) {
            onFinish();
            return this;
        }
        stopTimeInFuture = SystemClock.elapsedRealtime() + millisInFuture;
        handler.post(tickRunnable);
        return this;
    }

    public abstract void onTick(long millisUntilFinished);
    public abstract void onFinish();
}
