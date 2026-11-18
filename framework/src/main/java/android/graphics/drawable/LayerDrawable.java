package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class LayerDrawable extends Drawable implements Drawable.Callback {
    public static final int PADDING_MODE_NEST = 0;
    public static final int PADDING_MODE_STACK = 1;
    public static final int INSET_UNDEFINED = Integer.MIN_VALUE;

    private static final class Layer {
        Drawable drawable;
        int id = -1;
        int insetL, insetT, insetR, insetB;
        int width = -1, height = -1;

        Layer(Drawable d) {
            this.drawable = d;
        }
    }

    private final List<Layer> layers = new ArrayList<>();
    private int paddingMode = PADDING_MODE_NEST;

    public LayerDrawable(Drawable[] layers) {
        if (layers != null) {
            for (Drawable d : layers) {
                addLayer(d);
            }
        }
    }

    public int addLayer(Drawable dr) {
        Layer l = new Layer(dr);
        if (dr != null) dr.setCallback(this);
        layers.add(l);
        onBoundsChange(getBounds());
        return layers.size() - 1;
    }

    public int getNumberOfLayers() {
        return layers.size();
    }

    public Drawable getDrawable(int index) {
        return index >= 0 && index < layers.size() ? layers.get(index).drawable : null;
    }

    public void setDrawable(int index, Drawable drawable) {
        if (index < 0 || index >= layers.size()) return;
        Layer l = layers.get(index);
        if (l.drawable != null) l.drawable.setCallback(null);
        l.drawable = drawable;
        if (drawable != null) drawable.setCallback(this);
        onBoundsChange(getBounds());
        invalidateSelf();
    }

    public void setId(int index, int id) {
        if (index >= 0 && index < layers.size()) layers.get(index).id = id;
    }

    public int getId(int index) {
        return index >= 0 && index < layers.size() ? layers.get(index).id : -1;
    }

    public int findIndexByLayerId(int id) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).id == id) return i;
        }
        return -1;
    }

    public Drawable findDrawableByLayerId(int id) {
        int idx = findIndexByLayerId(id);
        return idx >= 0 ? layers.get(idx).drawable : null;
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        int idx = findIndexByLayerId(id);
        if (idx < 0) return false;
        setDrawable(idx, drawable);
        return true;
    }

    public void setLayerInset(int index, int l, int t, int r, int b) {
        if (index < 0 || index >= layers.size()) return;
        Layer layer = layers.get(index);
        layer.insetL = l;
        layer.insetT = t;
        layer.insetR = r;
        layer.insetB = b;
        onBoundsChange(getBounds());
    }

    public void setLayerInsetRelative(int index, int s, int t, int e, int b) {
        setLayerInset(index, s, t, e, b);
    }

    public void setLayerSize(int index, int w, int h) {
        if (index < 0 || index >= layers.size()) return;
        layers.get(index).width = w;
        layers.get(index).height = h;
    }

    public void setPaddingMode(int mode) {
        this.paddingMode = mode;
    }

    public int getPaddingMode() {
        return paddingMode;
    }

    @Override
    public void draw(Canvas canvas) {
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds == null) return;
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.setBounds(
                        bounds.left + l.insetL,
                        bounds.top + l.insetT,
                        bounds.right - l.insetR,
                        bounds.bottom - l.insetB);
            }
        }
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = false;
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.isStateful() && l.drawable.setState(state)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean changed = false;
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.setLevel(level)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean isStateful() {
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.isStateful()) return true;
        }
        return super.isStateful();
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.setTintList(tint);
        }
    }

    @Override
    public void jumpToCurrentState() {
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.jumpToCurrentState();
        }
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        for (Layer l : layers) {
            if (l.drawable != null) l.drawable.applyTheme(t);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        int w = -1;
        for (Layer l : layers) {
            if (l.drawable != null) w = Math.max(w, l.drawable.getIntrinsicWidth() + l.insetL + l.insetR);
        }
        return w;
    }

    @Override
    public int getIntrinsicHeight() {
        int h = -1;
        for (Layer l : layers) {
            if (l.drawable != null) h = Math.max(h, l.drawable.getIntrinsicHeight() + l.insetT + l.insetB);
        }
        return h;
    }

    @Override
    public void getOutline(Outline outline) {
        if (!layers.isEmpty() && layers.get(0).drawable != null) {
            layers.get(0).drawable.getOutline(outline);
        } else {
            super.getOutline(outline);
        }
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
