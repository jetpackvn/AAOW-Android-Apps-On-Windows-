package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public abstract class Animation implements Cloneable {
    public interface AnimationListener {
        void onAnimationStart(Animation animation);

        void onAnimationEnd(Animation animation);

        void onAnimationRepeat(Animation animation);
    }

    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int START_ON_FIRST_FRAME = -1;
    public static final int ABSOLUTE = 0;
    public static final int RELATIVE_TO_SELF = 1;
    public static final int RELATIVE_TO_PARENT = 2;
    public static final int ZORDER_NORMAL = 0;
    public static final int ZORDER_TOP = 1;
    public static final int ZORDER_BOTTOM = -1;

    private long duration = 300L;
    private long startOffset = 0L;
    private long startTime = -1L;
    private int repeatCount = 0;
    private int repeatMode = RESTART;
    private boolean fillBefore = true;
    private boolean fillAfter = false;
    private boolean fillEnabled = false;
    private boolean initialized = false;
    private boolean started = false;
    private boolean ended = false;
    private float scaleFactor = 1.0f;
    private Interpolator interpolator;
    private AnimationListener listener;

    public Animation() {
        ensureInterpolator();
    }

    public Animation(Context context, AttributeSet attrs) {
        ensureInterpolator();
    }

    protected void ensureInterpolator() {
        if (interpolator == null) {
            interpolator = new AccelerateDecelerateInterpolator();
        }
    }

    public void reset() {
        initialized = false;
        started = false;
        ended = false;
        startTime = -1L;
    }

    public void cancel() {
        if (started && !ended) {
            ended = true;
            if (listener != null) listener.onAnimationEnd(this);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        initialized = true;
    }

    public void setInterpolator(Context context, int resID) {
        ensureInterpolator();
    }

    public void setInterpolator(Interpolator i) {
        this.interpolator = i != null ? i : new LinearInterpolator();
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = Math.max(0L, startOffset);
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setDuration(long durationMillis) {
        this.duration = Math.max(0L, durationMillis);
    }

    public long getDuration() {
        return duration;
    }

    public void restrictDuration(long durationMillis) {
        if (duration > durationMillis) duration = durationMillis;
    }

    public void scaleCurrentDuration(float scale) {
        duration = Math.round(duration * scale);
    }

    public void setStartTime(long startTimeMillis) {
        this.startTime = startTimeMillis;
        this.started = false;
        this.ended = false;
    }

    public long getStartTime() {
        return startTime;
    }

    public void start() {
        setStartTime(-1L);
    }

    public void startNow() {
        setStartTime(AnimationUtils.currentAnimationTimeMillis());
    }

    public void setRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public boolean isFillEnabled() {
        return fillEnabled;
    }

    public void setFillEnabled(boolean fillEnabled) {
        this.fillEnabled = fillEnabled;
    }

    public void setFillBefore(boolean fillBefore) {
        this.fillBefore = fillBefore;
    }

    public boolean getFillBefore() {
        return fillBefore;
    }

    public void setFillAfter(boolean fillAfter) {
        this.fillAfter = fillAfter;
    }

    public boolean getFillAfter() {
        return fillAfter;
    }

    public void setAnimationListener(AnimationListener listener) {
        this.listener = listener;
    }

    protected float getScaleFactor() {
        return scaleFactor;
    }

    public long computeDurationHint() {
        return (startOffset + duration) * (repeatCount == INFINITE ? 1 : (repeatCount + 1L));
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation) {
        return getTransformation(currentTime, outTransformation, 1.0f);
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation, float scale) {
        this.scaleFactor = scale;
        if (startTime == -1L) {
            startTime = currentTime;
        }
        long elapsed = currentTime - (startTime + startOffset);
        if (elapsed < 0) {
            return true;
        }
        if (!started) {
            started = true;
            if (listener != null) listener.onAnimationStart(this);
        }

        float normalized;
        if (duration <= 0L) {
            normalized = 1.0f;
            ended = true;
        } else {
            int iter = (int) (elapsed / duration);
            if (repeatCount != INFINITE && iter > repeatCount) {
                normalized = (repeatMode == REVERSE && (repeatCount % 2 == 1)) ? 0.0f : 1.0f;
                if (!ended) {
                    ended = true;
                    if (listener != null) listener.onAnimationEnd(this);
                }
            } else {
                float raw = (elapsed % duration) / (float) duration;
                normalized = (repeatMode == REVERSE && (iter % 2 == 1)) ? (1.0f - raw) : raw;
            }
        }
        float interpolated = interpolator != null ? interpolator.getInterpolation(normalized) : normalized;
        if (outTransformation != null) {
            applyTransformation(interpolated, outTransformation);
        }
        return !ended;
    }

    protected void applyTransformation(float interpolatedTime, Transformation t) {
    }

    public boolean hasStarted() {
        return started;
    }

    public boolean hasEnded() {
        return ended;
    }

    public boolean willChangeTransformationMatrix() {
        return true;
    }

    public boolean willChangeBounds() {
        return true;
    }

    protected float resolveSize(int type, float value, int size, int parentSize) {
        return switch (type) {
            case RELATIVE_TO_SELF -> size * value;
            case RELATIVE_TO_PARENT -> parentSize * value;
            default -> value;
        };
    }

    @Override
    protected Animation clone() throws CloneNotSupportedException {
        return (Animation) super.clone();
    }
}
