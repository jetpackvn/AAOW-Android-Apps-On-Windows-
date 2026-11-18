package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Rect;

public abstract class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable drawable;

    public DrawableWrapper(Drawable dr) {
        setDrawable(dr);
    }

    public void setDrawable(Drawable dr) {
        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }
        this.drawable = dr;
        if (dr != null) {
            dr.setCallback(this);
            dr.setVisible(isVisible(), true);
            dr.setState(getState());
            dr.setLevel(getLevel());
            dr.setBounds(getBounds());
            dr.setLayoutDirection(getLayoutDirection());
        }
        invalidateSelf();
    }

    public Drawable getDrawable() {
        return drawable;
    }

    @Override
    public void draw(Canvas canvas) {
        if (drawable != null) drawable.draw(canvas);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | (drawable != null ? drawable.getChangingConfigurations() : 0);
    }

    @Override
    public boolean getPadding(Rect padding) {
        return drawable != null && drawable.getPadding(padding);
    }

    @Override
    public void getOutline(Outline outline) {
        if (drawable != null) drawable.getOutline(outline);
        else super.getOutline(outline);
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        if (drawable != null) drawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        if (drawable != null) drawable.setColorFilter(colorFilter);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        if (drawable != null) drawable.setTintList(tint);
    }

    @Override
    public boolean isStateful() {
        return drawable != null && drawable.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return drawable != null && drawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return drawable != null && drawable.setLevel(level);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (drawable != null) drawable.setBounds(bounds);
    }

    @Override
    public int getIntrinsicWidth() {
        return drawable != null ? drawable.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return drawable != null ? drawable.getIntrinsicHeight() : -1;
    }

    @Override
    public void jumpToCurrentState() {
        if (drawable != null) drawable.jumpToCurrentState();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        if (drawable != null) drawable.applyTheme(t);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
