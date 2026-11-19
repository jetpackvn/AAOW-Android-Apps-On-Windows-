package android.animation;

import java.util.ArrayList;
import java.util.List;

public class LayoutTransition {
    public static final int CHANGE_APPEARING = 0;
    public static final int CHANGE_DISAPPEARING = 1;
    public static final int APPEARING = 2;
    public static final int DISAPPEARING = 3;
    public static final int CHANGING = 4;

    public interface TransitionListener {
        void startTransition(LayoutTransition transition, android.view.ViewGroup container, android.view.View view, int transitionType);
        void endTransition(LayoutTransition transition, android.view.ViewGroup container, android.view.View view, int transitionType);
    }

    private long duration = 300L;
    private boolean animateParentHierarchy = true;
    private int transitionTypes = (1 << CHANGE_APPEARING) | (1 << CHANGE_DISAPPEARING) | (1 << APPEARING) | (1 << DISAPPEARING);
    private final List<TransitionListener> listeners = new ArrayList<>();

    public LayoutTransition() {
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setDuration(int transitionType, long duration) {
        this.duration = duration;
    }

    public long getDuration(int transitionType) {
        return duration;
    }

    public void setStartDelay(int transitionType, long delay) {
    }

    public long getStartDelay(int transitionType) {
        return 0L;
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
    }

    public TimeInterpolator getInterpolator(int transitionType) {
        return null;
    }

    public void setAnimator(int transitionType, Animator animator) {
    }

    public Animator getAnimator(int transitionType) {
        return null;
    }

    public void enableTransitionType(int transitionType) {
        transitionTypes |= (1 << transitionType);
    }

    public void disableTransitionType(int transitionType) {
        transitionTypes &= ~(1 << transitionType);
    }

    public boolean isTransitionTypeEnabled(int transitionType) {
        return (transitionTypes & (1 << transitionType)) != 0;
    }

    public void setAnimateParentHierarchy(boolean animateParentHierarchy) {
        this.animateParentHierarchy = animateParentHierarchy;
    }

    public boolean isChangingLayout() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public void addTransitionListener(TransitionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeTransitionListener(TransitionListener listener) {
        listeners.remove(listener);
    }

    public List<TransitionListener> getTransitionListeners() {
        return new ArrayList<>(listeners);
    }
}
