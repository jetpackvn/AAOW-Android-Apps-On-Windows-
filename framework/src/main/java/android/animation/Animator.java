package android.animation;

import java.util.ArrayList;
import java.util.List;

public abstract class Animator implements Cloneable {
    public static final long DURATION_INFINITE = -1L;

    public interface AnimatorListener {
        default void onAnimationStart(Animator animation, boolean isReverse) {
            onAnimationStart(animation);
        }

        default void onAnimationEnd(Animator animation, boolean isReverse) {
            onAnimationEnd(animation);
        }

        void onAnimationStart(Animator animation);

        void onAnimationEnd(Animator animation);

        void onAnimationCancel(Animator animation);

        void onAnimationRepeat(Animator animation);
    }

    public interface AnimatorPauseListener {
        void onAnimationPause(Animator animation);

        void onAnimationResume(Animator animation);
    }

    private final ArrayList<AnimatorListener> listeners = new ArrayList<>();
    private final ArrayList<AnimatorPauseListener> pauseListeners = new ArrayList<>();
    private boolean paused;

    public void start() {
    }

    public void cancel() {
    }

    public void end() {
    }

    public void pause() {
        if (isStarted() && !paused) {
            paused = true;
            for (AnimatorPauseListener l : List.copyOf(pauseListeners)) {
                l.onAnimationPause(this);
            }
        }
    }

    public void resume() {
        if (paused) {
            paused = false;
            for (AnimatorPauseListener l : List.copyOf(pauseListeners)) {
                l.onAnimationResume(this);
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public abstract long getStartDelay();

    public abstract void setStartDelay(long startDelay);

    public abstract Animator setDuration(long duration);

    public abstract long getDuration();

    public long getTotalDuration() {
        long duration = getDuration();
        if (duration == DURATION_INFINITE) {
            return DURATION_INFINITE;
        }
        return getStartDelay() + duration;
    }

    public abstract void setInterpolator(TimeInterpolator value);

    public TimeInterpolator getInterpolator() {
        return null;
    }

    public abstract boolean isRunning();

    public boolean isStarted() {
        return isRunning();
    }

    public void addListener(AnimatorListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(AnimatorListener listener) {
        listeners.remove(listener);
    }

    public ArrayList<AnimatorListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    public void addPauseListener(AnimatorPauseListener listener) {
        if (listener != null && !pauseListeners.contains(listener)) {
            pauseListeners.add(listener);
        }
    }

    public void removePauseListener(AnimatorPauseListener listener) {
        pauseListeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
        pauseListeners.clear();
    }

    public void setupStartValues() {
    }

    public void setupEndValues() {
    }

    public void setTarget(Object target) {
    }

    protected void notifyStart(boolean isReverse) {
        for (AnimatorListener l : List.copyOf(listeners)) {
            l.onAnimationStart(this, isReverse);
        }
    }

    protected void notifyEnd(boolean isReverse) {
        for (AnimatorListener l : List.copyOf(listeners)) {
            l.onAnimationEnd(this, isReverse);
        }
    }

    protected void notifyCancel() {
        for (AnimatorListener l : List.copyOf(listeners)) {
            l.onAnimationCancel(this);
        }
    }

    protected void notifyRepeat() {
        for (AnimatorListener l : List.copyOf(listeners)) {
            l.onAnimationRepeat(this);
        }
    }

    @Override
    public Animator clone() {
        try {
            Animator copy = (Animator) super.clone();
            copy.listeners.clear();
            copy.listeners.addAll(this.listeners);
            copy.pauseListeners.clear();
            copy.pauseListeners.addAll(this.pauseListeners);
            copy.paused = false;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
