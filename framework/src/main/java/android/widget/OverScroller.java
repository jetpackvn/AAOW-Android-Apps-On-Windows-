package android.widget;

import android.content.Context;
import android.os.SystemClock;
import android.view.animation.Interpolator;

public class OverScroller {
    private int startX, startY;
    private int finalX, finalY;
    private int currX, currY;
    private long startTime;
    private int duration;
    private boolean finished = true;
    private Interpolator interpolator;

    public OverScroller(Context context) {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator) {
        this.interpolator = interpolator != null ? interpolator : t -> t;
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX, float bounceCoefficientY) {
        this(context, interpolator);
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX, float bounceCoefficientY, boolean flywheel) {
        this(context, interpolator);
    }

    public final void setFriction(float friction) {
    }

    public final boolean isFinished() {
        return finished;
    }

    public final void forceFinished(boolean finished) {
        this.finished = finished;
    }

    public final int getCurrX() {
        return currX;
    }

    public final int getCurrY() {
        return currY;
    }

    public float getCurrVelocity() {
        return finished ? 0f : 100f;
    }

    public final int getStartX() {
        return startX;
    }

    public final int getStartY() {
        return startY;
    }

    public final int getFinalX() {
        return finalX;
    }

    public final int getFinalY() {
        return finalY;
    }

    public void setFinalX(int newX) {
        this.finalX = newX;
        this.finished = false;
    }

    public void setFinalY(int newY) {
        this.finalY = newY;
        this.finished = false;
    }

    public boolean computeScrollOffset() {
        if (finished) return false;
        long elapsed = SystemClock.uptimeMillis() - startTime;
        if (elapsed < duration && duration > 0) {
            float x = interpolator.getInterpolation(elapsed / (float) duration);
            currX = startX + Math.round(x * (finalX - startX));
            currY = startY + Math.round(x * (finalY - startY));
        } else {
            currX = finalX;
            currY = finalY;
            finished = true;
        }
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, 250);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        this.finished = false;
        this.duration = Math.max(0, duration);
        this.startTime = SystemClock.uptimeMillis();
        this.startX = this.currX = startX;
        this.startY = this.currY = startY;
        this.finalX = startX + dx;
        this.finalY = startY + dy;
    }

    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        int fx = Math.max(minX, Math.min(maxX, startX));
        int fy = Math.max(minY, Math.min(maxY, startY));
        if (fx != startX || fy != startY) {
            startScroll(startX, startY, fx - startX, fy - startY, 200);
            return true;
        }
        finished = true;
        return false;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY) {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY, int overX, int overY) {
        int dx = Math.round(velocityX * 0.25f);
        int dy = Math.round(velocityY * 0.25f);
        int fx = Math.max(minX, Math.min(maxX, startX + dx));
        int fy = Math.max(minY, Math.min(maxY, startY + dy));
        startScroll(startX, startY, fx - startX, fy - startY, 350);
    }

    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
    }

    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
    }

    public boolean isOverScrolled() {
        return false;
    }

    public void abortAnimation() {
        currX = finalX;
        currY = finalY;
        finished = true;
    }
}
