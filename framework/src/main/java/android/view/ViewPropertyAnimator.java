package android.view;

public class ViewPropertyAnimator {
    private final View view;
    private long duration = 250;
    private long startDelay;
    private Runnable endAction;

    public ViewPropertyAnimator(View view) {
        this.view = view;
    }

    public ViewPropertyAnimator setDuration(long duration) {
        this.duration = Math.max(0, duration);
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public ViewPropertyAnimator setStartDelay(long startDelay) {
        this.startDelay = Math.max(0, startDelay);
        return this;
    }

    public long getStartDelay() {
        return startDelay;
    }

    public ViewPropertyAnimator alpha(float value) {
        if (view != null) view.setAlpha(value);
        return this;
    }

    public ViewPropertyAnimator alphaBy(float value) {
        if (view != null) view.setAlpha(view.getAlpha() + value);
        return this;
    }

    public ViewPropertyAnimator translationX(float value) {
        if (view != null) view.setTranslationX(value);
        return this;
    }

    public ViewPropertyAnimator translationY(float value) {
        if (view != null) view.setTranslationY(value);
        return this;
    }

    public ViewPropertyAnimator scaleX(float value) {
        if (view != null) view.setScaleX(value);
        return this;
    }

    public ViewPropertyAnimator scaleY(float value) {
        if (view != null) view.setScaleY(value);
        return this;
    }

    public ViewPropertyAnimator rotation(float value) {
        if (view != null) view.setRotation(value);
        return this;
    }

    public ViewPropertyAnimator withEndAction(Runnable runnable) {
        this.endAction = runnable;
        return this;
    }

    public ViewPropertyAnimator withStartAction(Runnable runnable) {
        if (runnable != null) runnable.run();
        return this;
    }

    public void start() {
        if (endAction != null) {
            Runnable r = endAction;
            endAction = null;
            r.run();
        }
    }

    public void cancel() {
        endAction = null;
    }
}
