package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class DrawableContainer extends Drawable implements Drawable.Callback {
    public static abstract class DrawableContainerState extends ConstantState {
        final List<Drawable> children = new ArrayList<>();

        public final int addChild(Drawable dr) {
            int pos = children.size();
            children.add(dr);
            return pos;
        }

        public final int getChildCount() {
            return children.size();
        }

        public final Drawable getChild(int index) {
            return index >= 0 && index < children.size() ? children.get(index) : null;
        }
    }

    private DrawableContainerState state;
    private Drawable currDrawable;
    private int curIndex = -1;

    @Override
    public void draw(Canvas canvas) {
        if (currDrawable != null) currDrawable.draw(canvas);
    }

    public boolean selectDrawable(int index) {
        if (index == curIndex) return false;
        if (state != null && index >= 0 && index < state.getChildCount()) {
            Drawable d = state.getChild(index);
            curIndex = index;
            currDrawable = d;
            if (d != null) {
                d.setCallback(this);
                d.setBounds(getBounds());
                d.setState(getState());
                d.setAlpha(getAlpha());
                d.setColorFilter(getColorFilter());
                d.setVisible(isVisible(), true);
            }
            invalidateSelf();
            return true;
        }
        curIndex = -1;
        currDrawable = null;
        return false;
    }

    @Override
    public Drawable getCurrent() {
        return currDrawable != null ? currDrawable : this;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (currDrawable != null) currDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return currDrawable != null && currDrawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return currDrawable != null && currDrawable.setLevel(level);
    }

    @Override
    public int getIntrinsicWidth() {
        return currDrawable != null ? currDrawable.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return currDrawable != null ? currDrawable.getIntrinsicHeight() : -1;
    }

    @Override
    public boolean getPadding(Rect padding) {
        return currDrawable != null && currDrawable.getPadding(padding);
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        if (currDrawable != null) currDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        if (currDrawable != null) currDrawable.setColorFilter(colorFilter);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        if (currDrawable != null) currDrawable.setTintList(tint);
    }

    @Override
    public void jumpToCurrentState() {
        if (currDrawable != null) currDrawable.jumpToCurrentState();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        if (currDrawable != null) currDrawable.applyTheme(t);
    }

    protected void setConstantState(DrawableContainerState state) {
        this.state = state;
        if (state != null && state.getChildCount() > 0 && curIndex < 0) {
            selectDrawable(0);
        }
    }

    public void setEnterFadeDuration(int ms) {
    }

    public void setExitFadeDuration(int ms) {
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        if (who == currDrawable) invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who == currDrawable) scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (who == currDrawable) unscheduleSelf(what);
    }
}
