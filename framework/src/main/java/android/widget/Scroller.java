package android.widget;

import android.content.Context;
import android.view.animation.Interpolator;

public class Scroller {
    private final OverScroller delegate;

    public Scroller(Context context) {
        this(context, null);
    }

    public Scroller(Context context, Interpolator interpolator) {
        this(context, interpolator, true);
    }

    public Scroller(Context context, Interpolator interpolator, boolean flywheel) {
        this.delegate = new OverScroller(context, interpolator);
    }

    public final void setFriction(float friction) {
        delegate.setFriction(friction);
    }

    public final boolean isFinished() {
        return delegate.isFinished();
    }

    public final void forceFinished(boolean finished) {
        delegate.forceFinished(finished);
    }

    public final int getDuration() {
        return 250;
    }

    public final int getCurrX() {
        return delegate.getCurrX();
    }

    public final int getCurrY() {
        return delegate.getCurrY();
    }

    public float getCurrVelocity() {
        return delegate.getCurrVelocity();
    }

    public final int getStartX() {
        return delegate.getStartX();
    }

    public final int getStartY() {
        return delegate.getStartY();
    }

    public final int getFinalX() {
        return delegate.getFinalX();
    }

    public final int getFinalY() {
        return delegate.getFinalY();
    }

    public void setFinalX(int newX) {
        delegate.setFinalX(newX);
    }

    public void setFinalY(int newY) {
        delegate.setFinalY(newY);
    }

    public boolean computeScrollOffset() {
        return delegate.computeScrollOffset();
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        delegate.startScroll(startX, startY, dx, dy);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        delegate.startScroll(startX, startY, dx, dy, duration);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY) {
        delegate.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
    }

    public void extendDuration(int extend) {
    }

    public int timePassed() {
        return 0;
    }

    public void abortAnimation() {
        delegate.abortAnimation();
    }
}
