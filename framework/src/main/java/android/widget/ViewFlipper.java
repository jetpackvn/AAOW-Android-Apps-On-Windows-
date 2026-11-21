package android.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

/**
 * Cài đặt thật android.widget.ViewFlipper kế thừa ViewAnimator (giữ nguyên Context/AttributeSet,
 * hỗ trợ autoStart, flipInterval và startFlipping/stopFlipping).
 */
public class ViewFlipper extends ViewAnimator {
    private static final int DEFAULT_INTERVAL = 3000;

    private int flipInterval = DEFAULT_INTERVAL;
    private boolean autoStart = false;
    private boolean started = false;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable flipRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                showNext();
                handler.postDelayed(this, flipInterval);
            }
        }
    };

    public ViewFlipper(Context context) {
        super(context);
    }

    public ViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFlipInterval(int milliseconds) {
        this.flipInterval = Math.max(50, milliseconds);
    }

    public int getFlipInterval() {
        return flipInterval;
    }

    public void startFlipping() {
        started = true;
        updateRunning();
    }

    public void stopFlipping() {
        started = false;
        updateRunning();
    }

    public boolean isFlipping() {
        return started;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    private void updateRunning() {
        boolean shouldRun = started && isAttachedToWindow() && getVisibility() == VISIBLE;
        if (shouldRun != running) {
            running = shouldRun;
            handler.removeCallbacks(flipRunnable);
            if (running) {
                handler.postDelayed(flipRunnable, flipInterval);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoStart) {
            startFlipping();
        } else {
            updateRunning();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        handler.removeCallbacks(flipRunnable);
    }

    @Override
    protected void onVisibilityChanged(android.view.View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateRunning();
    }
}
