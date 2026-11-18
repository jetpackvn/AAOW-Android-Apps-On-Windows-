package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class Drawable {
    private final Rect bounds = new Rect();
    private int alpha = 255;

    public abstract void draw(Canvas canvas);

    public void setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
    }

    public int getAlpha() {
        return alpha;
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

    public boolean getPadding(Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }
}
