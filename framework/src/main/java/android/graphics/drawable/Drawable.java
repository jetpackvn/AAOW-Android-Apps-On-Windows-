package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
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

        public boolean canApplyTheme() {
            return false;
        }
    }

    private final Rect bounds = new Rect();
    private int alpha = 255;
    private int level;
    private boolean visible = true;
    private int[] stateSet = new int[0];
    private WeakReference<Callback> callback;
    private ColorStateList tintList;
    private ColorFilter colorFilter;
    private int changingConfigurations;
    private int layoutDirection;
    private boolean autoMirrored;

    public abstract void draw(Canvas canvas);

    public void setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
        invalidateSelf();
    }

    public int getAlpha() {
        return alpha;
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
        invalidateSelf();
    }

    public void setColorFilter(int color, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public void clearColorFilter() {
        setColorFilter(null);
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

    protected ColorStateList getTintListInternal() {
        return tintList;
    }

    protected int resolveTintedColor(int baseColor) {
        int col = baseColor;
        if (tintList != null) {
            int tintCol = tintList.getColorForState(stateSet, tintList.getDefaultColor());
            int ba = android.graphics.Color.alpha(col);
            int ta = android.graphics.Color.alpha(tintCol);
            if (ba == 0) ba = 255;
            col = android.graphics.Color.argb((ba * ta) / 255,
                    android.graphics.Color.red(tintCol),
                    android.graphics.Color.green(tintCol),
                    android.graphics.Color.blue(tintCol));
        }
        if (colorFilter != null) {
            col = Canvas.applyColorFilter(col, colorFilter);
        }
        if (alpha < 255) {
            int a = (android.graphics.Color.alpha(col) * alpha) / 255;
            col = (col & 0x00FFFFFF) | (a << 24);
        }
        return col;
    }

    public void setBounds(int left, int top, int right, int bottom) {
        boolean changed = bounds.left != left || bounds.top != top
                || bounds.right != right || bounds.bottom != bottom;
        bounds.set(left, top, right, bottom);
        if (changed) {
            onBoundsChange(bounds);
        }
    }

    public void setBounds(Rect bounds) {
        if (bounds != null) {
            setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
        }
    }

    public final Rect getBounds() {
        return bounds;
    }

    public final Rect copyBounds() {
        return new Rect(bounds);
    }

    public final void copyBounds(Rect bounds) {
        if (bounds != null) bounds.set(this.bounds);
    }

    protected void onBoundsChange(Rect bounds) {
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
        if (padding != null) padding.set(0, 0, 0, 0);
        return false;
    }

    public android.graphics.Insets getOpticalInsets() {
        return android.graphics.Insets.NONE;
    }

    public void getOutline(Outline outline) {
        if (outline != null) {
            outline.setRect(getBounds());
            outline.setAlpha(getAlpha() / 255.0f);
        }
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setDither(boolean dither) {
    }

    public void setFilterBitmap(boolean filter) {
    }

    public boolean isFilterBitmap() {
        return false;
    }

    public void setAutoMirrored(boolean mirrored) {
        this.autoMirrored = mirrored;
    }

    public boolean isAutoMirrored() {
        return autoMirrored;
    }

    public int getLayoutDirection() {
        return layoutDirection;
    }

    public final boolean setLayoutDirection(int layoutDirection) {
        if (this.layoutDirection != layoutDirection) {
            this.layoutDirection = layoutDirection;
            return onLayoutDirectionChanged(layoutDirection);
        }
        return false;
    }

    public boolean onLayoutDirectionChanged(int layoutDirection) {
        return false;
    }

    public int getChangingConfigurations() {
        return changingConfigurations;
    }

    public void setChangingConfigurations(int configs) {
        this.changingConfigurations = configs;
    }

    public void applyTheme(Resources.Theme t) {
    }

    public boolean canApplyTheme() {
        return false;
    }

    public void setHotspot(float x, float y) {
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
    }

    public void getHotspotBounds(Rect outRect) {
        if (outRect != null) outRect.set(getBounds());
    }

    public void jumpToCurrentState() {
    }

    public Drawable getCurrent() {
        return this;
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

    public boolean hasFocusStateSpecified() {
        return false;
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

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        inflate(r, parser, attrs, null);
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme)
            throws XmlPullParserException, IOException {
    }

    public Drawable mutate() {
        return this;
    }

    public ConstantState getConstantState() {
        final Drawable self = this;
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return self;
            }
        };
    }
}
