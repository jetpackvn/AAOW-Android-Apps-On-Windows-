package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;

import java.lang.ref.WeakReference;

public abstract class Drawable {
    public interface Callback {
        void invalidateDrawable(Drawable who);

        void scheduleDrawable(Drawable who, Runnable what, long when);

        void unscheduleDrawable(Drawable who, Runnable what);
    }

    public static abstract class ConstantState {
        public abstract Drawable newDrawable();

        public Drawable newDrawable(Resources res) {
            return newDrawable();
        }

        public Drawable newDrawable(Resources res, Resources.Theme theme) {
            return newDrawable(res);
        }

        public int getChangingConfigurations() {
            return 0;
        }
    }

    private final Rect bounds = new Rect();
    private int alpha = 255;
    private int level;
    private boolean visible = true;
    private int[] stateSet = new int[0];
    private WeakReference<Callback> callback;
    private ColorStateList tintList;

    public abstract void draw(Canvas canvas);

    public void setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
        invalidateSelf();
    }

    public int getAlpha() {
        return alpha;
    }

    public void setColorFilter(int color, PorterDuff.Mode mode) {
        invalidateSelf();
    }

    public void clearColorFilter() {
        invalidateSelf();
    }

    public void setTint(int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    public void setTintList(ColorStateList tint) {
        this.tintList = tint;
        invalidateSelf();
    }

    public void setTintMode(PorterDuff.Mode tintMode) {
        invalidateSelf();
    }

    public void setBounds(int left, int top, int right, int bottom) {
        bounds.set(left, top, right, bottom);
    }

    public void setBounds(Rect bounds) {
        if (bounds != null) this.bounds.set(bounds);
    }

    public final Rect getBounds() {
        return bounds;
    }

    public int getIntrinsicWidth() {
        return -1;
    }

    public int getIntrinsicHeight() {
        return -1;
    }

    public int getMinimumWidth() {
        int w = getIntrinsicWidth();
        return Math.max(0, w);
    }

    public int getMinimumHeight() {
        int h = getIntrinsicHeight();
        return Math.max(0, h);
    }

    public boolean getPadding(Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }

    public void setCallback(Callback cb) {
        this.callback = cb != null ? new WeakReference<>(cb) : null;
    }

    public Callback getCallback() {
        return callback != null ? callback.get() : null;
    }

    public void invalidateSelf() {
        Callback cb = getCallback();
        if (cb != null) {
            cb.invalidateDrawable(this);
        }
    }

    public void scheduleSelf(Runnable what, long when) {
        Callback cb = getCallback();
        if (cb != null) {
            cb.scheduleDrawable(this, what, when);
        }
    }

    public void unscheduleSelf(Runnable what) {
        Callback cb = getCallback();
        if (cb != null) {
            cb.unscheduleDrawable(this, what);
        }
    }

    public boolean isStateful() {
        return tintList != null && tintList.isStateful();
    }

    public int[] getState() {
        return stateSet;
    }

    public boolean setState(int[] stateSet) {
        this.stateSet = stateSet != null ? stateSet : new int[0];
        return onStateChange(this.stateSet);
    }

    protected boolean onStateChange(int[] state) {
        return false;
    }

    public final int getLevel() {
        return level;
    }

    public final boolean setLevel(int level) {
        if (this.level != level) {
            this.level = level;
            return onLevelChange(level);
        }
        return false;
    }

    protected boolean onLevelChange(int level) {
        return false;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = this.visible != visible;
        this.visible = visible;
        return changed;
    }

    public final boolean isVisible() {
        return visible;
    }

    public Drawable mutate() {
        return this;
    }

    public ConstantState getConstantState() {
        return null;
    }
}
