package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Gravity;

public class ClipDrawable extends DrawableWrapper {
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    private final int gravity;
    private final int orientation;
    private final Rect tmpRect = new Rect();

    public ClipDrawable(Drawable drawable, int gravity, int orientation) {
        super(drawable);
        this.gravity = gravity;
        this.orientation = orientation;
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        invalidateSelf();
        return true;
    }

    @Override
    public int getOpacity() {
        Drawable d = getDrawable();
        if (d == null || getLevel() == 0) return android.graphics.PixelFormat.TRANSPARENT;
        return d.getOpacity();
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable d = getDrawable();
        if (d == null) return;
        int level = getLevel();
        if (level <= 0) return;
        if (level >= 10000) {
            d.draw(canvas);
            return;
        }
        Rect bounds = getBounds();
        int w = bounds.width();
        int h = bounds.height();
        if ((orientation & HORIZONTAL) != 0) {
            w = Math.max(0, w - (w * (10000 - level)) / 10000);
        }
        if ((orientation & VERTICAL) != 0) {
            h = Math.max(0, h - (h * (10000 - level)) / 10000);
        }
        if (w <= 0 || h <= 0) return;
        int left = bounds.left;
        int top = bounds.top;
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
            left = bounds.right - w;
        } else if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL) {
            left = bounds.left + (bounds.width() - w) / 2;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            top = bounds.bottom - h;
        } else if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
            top = bounds.top + (bounds.height() - h) / 2;
        }
        tmpRect.set(left, top, left + w, top + h);
        int save = canvas.save();
        canvas.clipRect(tmpRect);
        d.draw(canvas);
        canvas.restoreToCount(save);
    }
}
