package android.view;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;

/**
 * Cài đặt android.view.ViewPropertyAnimator điều khiển các thuộc tính View mượt ở 60fps.
 */
public class ViewPropertyAnimator {
    private final View view;
    private long duration = 250;
    private long startDelay;
    private TimeInterpolator interpolator;
    private Animator.AnimatorListener listener;
    private ValueAnimator.AnimatorUpdateListener updateListener;
    private Runnable startAction;
    private Runnable endAction;

    private boolean hasAlpha;
    private float targetAlpha;
    private boolean hasTx;
    private float targetTx;
    private boolean hasTy;
    private float targetTy;
    private boolean hasSx;
    private float targetSx;
    private boolean hasSy;
    private float targetSy;
    private boolean hasRot;
    private float targetRot;
    private ValueAnimator activeAnimator;

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

    public ViewPropertyAnimator setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    public ViewPropertyAnimator setListener(Animator.AnimatorListener listener) {
        this.listener = listener;
        return this;
    }

    public ViewPropertyAnimator setUpdateListener(ValueAnimator.AnimatorUpdateListener listener) {
        this.updateListener = listener;
        return this;
    }

    public ViewPropertyAnimator alpha(float value) {
        this.hasAlpha = true;
        this.targetAlpha = value;
        return this;
    }

    public ViewPropertyAnimator alphaBy(float value) {
        return alpha((view != null ? view.getAlpha() : 1f) + value);
    }

    public ViewPropertyAnimator translationX(float value) {
        this.hasTx = true;
        this.targetTx = value;
        return this;
    }

    public ViewPropertyAnimator translationXBy(float value) {
        return translationX((view != null ? view.getTranslationX() : 0f) + value);
    }

    public ViewPropertyAnimator translationY(float value) {
        this.hasTy = true;
        this.targetTy = value;
        return this;
    }

    public ViewPropertyAnimator translationYBy(float value) {
        return translationY((view != null ? view.getTranslationY() : 0f) + value);
    }

    public ViewPropertyAnimator translationZ(float value) {
        return this;
    }

    public ViewPropertyAnimator z(float value) {
        return this;
    }

    public ViewPropertyAnimator scaleX(float value) {
        this.hasSx = true;
        this.targetSx = value;
        return this;
    }

    public ViewPropertyAnimator scaleY(float value) {
        this.hasSy = true;
        this.targetSy = value;
        return this;
    }

    public ViewPropertyAnimator rotation(float value) {
        this.hasRot = true;
        this.targetRot = value;
        return this;
    }

    public ViewPropertyAnimator withEndAction(Runnable runnable) {
        this.endAction = runnable;
        return this;
    }

    public ViewPropertyAnimator withStartAction(Runnable runnable) {
        this.startAction = runnable;
        return this;
    }

    public void start() {
        cancel();
        if (view == null) return;

        final float startAlpha = view.getAlpha();
        final float startTx = view.getTranslationX();
        final float startTy = view.getTranslationY();
        final float startSx = view.getScaleX();
        final float startSy = view.getScaleY();
        final float startRot = view.getRotation();

        final boolean animateAlpha = hasAlpha;
        final boolean animateTx = hasTx;
        final boolean animateTy = hasTy;
        final boolean animateSx = hasSx;
        final boolean animateSy = hasSy;
        final boolean animateRot = hasRot;

        final float dstAlpha = targetAlpha;
        final float dstTx = targetTx;
        final float dstTy = targetTy;
        final float dstSx = targetSx;
        final float dstSy = targetSy;
        final float dstRot = targetRot;

        clearPendingProperties();

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        if (interpolator != null) anim.setInterpolator(interpolator);
        if (listener != null) anim.addListener(listener);

        if (startAction != null) {
            Runnable sa = startAction;
            startAction = null;
            sa.run();
        }

        Runnable capturedEnd = endAction;
        endAction = null;

        anim.addUpdateListener(va -> {
            float f = va.getAnimatedFraction();
            if (animateAlpha) view.setAlpha(startAlpha + (dstAlpha - startAlpha) * f);
            if (animateTx) view.setTranslationX(startTx + (dstTx - startTx) * f);
            if (animateTy) view.setTranslationY(startTy + (dstTy - startTy) * f);
            if (animateSx) view.setScaleX(startSx + (dstSx - startSx) * f);
            if (animateSy) view.setScaleY(startSy + (dstSy - startSy) * f);
            if (animateRot) view.setRotation(startRot + (dstRot - startRot) * f);
            if (updateListener != null) updateListener.onAnimationUpdate(va);
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (activeAnimator == anim) activeAnimator = null;
                if (capturedEnd != null) capturedEnd.run();
            }
        });
        this.activeAnimator = anim;
        anim.start();
    }

    public void cancel() {
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
        }
        clearPendingProperties();
    }

    private void clearPendingProperties() {
        hasAlpha = hasTx = hasTy = hasSx = hasSy = hasRot = false;
    }
}
