package android.animation;

import android.view.animation.AccelerateDecelerateInterpolator;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cài đặt android.animation.ValueAnimator chạy ở 60fps thông qua Swing Timer.
 */
public class ValueAnimator extends Animator {
    public interface AnimatorUpdateListener {
        void onAnimationUpdate(ValueAnimator animation);
    }

    public interface DurationScaleChangeListener {
        void onChanged(float scale);
    }

    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int INFINITE = -1;

    private static final List<ValueAnimator> ACTIVE_ANIMATORS = new CopyOnWriteArrayList<>();
    private static Timer frameTimer;
    private static long frameDelayMs = 16L;
    private static float durationScale = 1.0f;

    private long duration = 300L;
    private long startDelay = 0L;
    private int repeatCount = 0;
    private int repeatMode = RESTART;
    private TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();
    private PropertyValuesHolder[] values = new PropertyValuesHolder[0];
    private final List<AnimatorUpdateListener> updateListeners = new CopyOnWriteArrayList<>();

    private boolean started;
    private boolean running;
    private boolean reverse;
    private long startTimeNs;
    private long currentPlayTimeMs;
    private float animatedFraction;
    private int completedIterations;

    public ValueAnimator() {
    }

    public static ValueAnimator ofFloat(float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        return anim;
    }

    public static ValueAnimator ofInt(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        return anim;
    }

    public static ValueAnimator ofArgb(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    @SafeVarargs
    public static <T> ValueAnimator ofObject(TypeEvaluator<T> evaluator, T... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setObjectValues((Object[]) values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setValues(values);
        return anim;
    }

    public static boolean areAnimatorsEnabled() {
        return durationScale > 0f;
    }

    public static float getDurationScale() {
        return durationScale;
    }

    public static long getFrameDelay() {
        return frameDelayMs;
    }

    public static void setFrameDelay(long frameDelay) {
        frameDelayMs = Math.max(4L, frameDelay);
        if (frameTimer != null) {
            frameTimer.setDelay((int) frameDelayMs);
        }
    }

    public static boolean registerDurationScaleChangeListener(DurationScaleChangeListener listener) {
        return listener != null;
    }

    public static boolean unregisterDurationScaleChangeListener(DurationScaleChangeListener listener) {
        return listener != null;
    }

    public void setFloatValues(float... values) {
        if (this.values.length == 0) {
            setValues(PropertyValuesHolder.ofFloat("", values));
        } else {
            this.values[0].setFloatValues(values);
        }
    }

    public void setIntValues(int... values) {
        if (this.values.length == 0) {
            setValues(PropertyValuesHolder.ofInt("", values));
        } else {
            this.values[0].setIntValues(values);
        }
    }

    public void setObjectValues(Object... values) {
        if (this.values.length == 0) {
            setValues(PropertyValuesHolder.ofObject("", null, values));
        } else {
            this.values[0].setObjectValues(values);
        }
    }

    public void setValues(PropertyValuesHolder... values) {
        this.values = values != null ? values.clone() : new PropertyValuesHolder[0];
    }

    public PropertyValuesHolder[] getValues() {
        return values.clone();
    }

    public void setEvaluator(TypeEvaluator<?> value) {
        if (value != null && values.length > 0) {
            values[0].setEvaluator(value);
        }
    }

    @Override
    public ValueAnimator setDuration(long duration) {
        this.duration = Math.max(0L, duration);
        return this;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public long getTotalDuration() {
        if (repeatCount == INFINITE) return DURATION_INFINITE;
        return startDelay + duration * (repeatCount + 1L);
    }

    @Override
    public long getStartDelay() {
        return startDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        this.startDelay = Math.max(0L, startDelay);
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        this.interpolator = value != null ? value : t -> t;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    public void setRepeatCount(int value) {
        this.repeatCount = value;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatMode(int value) {
        this.repeatMode = value;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void addUpdateListener(AnimatorUpdateListener listener) {
        if (listener != null && !updateListeners.contains(listener)) {
            updateListeners.add(listener);
        }
    }

    public void removeUpdateListener(AnimatorUpdateListener listener) {
        updateListeners.remove(listener);
    }

    public void removeAllUpdateListeners() {
        updateListeners.clear();
    }

    public float getAnimatedFraction() {
        return animatedFraction;
    }

    public Object getAnimatedValue() {
        return values.length > 0 ? values[0].getAnimatedValue() : null;
    }

    public Object getAnimatedValue(String propertyName) {
        if (propertyName == null) return getAnimatedValue();
        for (PropertyValuesHolder h : values) {
            if (propertyName.equals(h.getPropertyName())) {
                return h.getAnimatedValue();
            }
        }
        return null;
    }

    public long getCurrentPlayTime() {
        return currentPlayTimeMs;
    }

    public void setCurrentPlayTime(long playTime) {
        long clamped = Math.max(0L, playTime);
        this.currentPlayTimeMs = clamped;
        this.startTimeNs = System.nanoTime() - (clamped + startDelay) * 1_000_000L;
        float frac = duration == 0L ? 1.0f : Math.min(1.0f, clamped / (float) duration);
        animateValue(frac);
    }

    public void setCurrentFraction(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        this.currentPlayTimeMs = Math.round(duration * f);
        animateValue(f);
    }

    protected void animateValue(float fraction) {
        float interp = interpolator != null ? interpolator.getInterpolation(fraction) : fraction;
        this.animatedFraction = interp;
        for (PropertyValuesHolder h : values) {
            h.calculateValue(interp);
        }
        for (AnimatorUpdateListener l : updateListeners) {
            l.onAnimationUpdate(this);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        start(false);
    }

    private void start(boolean playBackwards) {
        this.reverse = playBackwards;
        this.started = true;
        this.running = true;
        this.completedIterations = 0;
        this.currentPlayTimeMs = 0L;
        this.startTimeNs = System.nanoTime();
        setupStartValues();
        notifyStart(reverse);

        if (duration == 0L && startDelay == 0L) {
            animateValue(reverse ? 0f : 1f);
            finishAnimation();
            return;
        }
        if (startDelay == 0L) {
            animateValue(reverse ? 1f : 0f);
        }
        registerAnimator(this);
    }

    public void reverse() {
        if (started) {
            reverse = !reverse;
            startTimeNs = System.nanoTime() - Math.round((1f - animatedFraction) * duration) * 1_000_000L;
        } else {
            start(true);
        }
    }

    @Override
    public void cancel() {
        if (started || running) {
            ACTIVE_ANIMATORS.remove(this);
            stopTimerIfIdle();
            running = false;
            started = false;
            notifyCancel();
            notifyEnd(reverse);
        }
    }

    @Override
    public void end() {
        if (!started) {
            start();
        }
        if (started) {
            animateValue(reverse ? 0f : 1f);
            finishAnimation();
        }
    }

    private void finishAnimation() {
        ACTIVE_ANIMATORS.remove(this);
        stopTimerIfIdle();
        running = false;
        started = false;
        notifyEnd(reverse);
    }

    private boolean stepFrame(long nowNs) {
        if (!running || isPaused()) return true;
        long elapsedMs = (nowNs - startTimeNs) / 1_000_000L;
        if (elapsedMs < startDelay) {
            return true;
        }
        long playMs = elapsedMs - startDelay;
        this.currentPlayTimeMs = playMs;

        if (duration <= 0L) {
            animateValue(reverse ? 0f : 1f);
            finishAnimation();
            return false;
        }

        int iteration = (int) (playMs / duration);
        if (repeatCount != INFINITE && iteration > repeatCount) {
            boolean endReverse = (repeatMode == REVERSE) && (repeatCount % 2 == 1) ? !reverse : reverse;
            animateValue(endReverse ? 0f : 1f);
            finishAnimation();
            return false;
        }

        while (completedIterations < iteration) {
            completedIterations++;
            notifyRepeat();
        }

        float rawFraction = (playMs % duration) / (float) duration;
        boolean iterReverse = reverse ^ (repeatMode == REVERSE && (iteration % 2 == 1));
        animateValue(iterReverse ? (1f - rawFraction) : rawFraction);
        return true;
    }

    private static synchronized void registerAnimator(ValueAnimator anim) {
        if (!ACTIVE_ANIMATORS.contains(anim)) {
            ACTIVE_ANIMATORS.add(anim);
        }
        if (frameTimer == null) {
            frameTimer = new Timer((int) frameDelayMs, e -> tickAll());
            frameTimer.setCoalesce(true);
        }
        if (!frameTimer.isRunning()) {
            frameTimer.start();
        }
    }

    private static synchronized void stopTimerIfIdle() {
        if (ACTIVE_ANIMATORS.isEmpty() && frameTimer != null && frameTimer.isRunning()) {
            frameTimer.stop();
        }
    }

    private static void tickAll() {
        long now = System.nanoTime();
        for (ValueAnimator anim : ACTIVE_ANIMATORS) {
            anim.stepFrame(now);
        }
        stopTimerIfIdle();
    }

    @Override
    public ValueAnimator clone() {
        ValueAnimator copy = (ValueAnimator) super.clone();
        copy.updateListeners.clear();
        copy.updateListeners.addAll(this.updateListeners);
        copy.values = new PropertyValuesHolder[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            copy.values[i] = this.values[i].clone();
        }
        copy.started = false;
        copy.running = false;
        return copy;
    }
}
