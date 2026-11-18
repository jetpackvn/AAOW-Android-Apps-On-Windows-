package android.graphics.drawable;

import android.graphics.Rect;

public class InsetDrawable extends DrawableWrapper {
    private final int insetLeft;
    private final int insetTop;
    private final int insetRight;
    private final int insetBottom;

    public InsetDrawable(Drawable drawable, int inset) {
        this(drawable, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable drawable, float inset) {
        this(drawable, Math.round(inset));
    }

    public InsetDrawable(Drawable drawable, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        super(drawable);
        this.insetLeft = insetLeft;
        this.insetTop = insetTop;
        this.insetRight = insetRight;
        this.insetBottom = insetBottom;
    }

    public InsetDrawable(Drawable drawable, float insetLeft, float insetTop, float insetRight, float insetBottom) {
        this(drawable, Math.round(insetLeft), Math.round(insetTop), Math.round(insetRight), Math.round(insetBottom));
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Drawable d = getDrawable();
        if (d != null && bounds != null) {
            d.setBounds(bounds.left + insetLeft, bounds.top + insetTop,
                    bounds.right - insetRight, bounds.bottom - insetBottom);
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        boolean pad = super.getPadding(padding);
        if (padding != null) {
            padding.left += insetLeft;
            padding.top += insetTop;
            padding.right += insetRight;
            padding.bottom += insetBottom;
        }
        return pad || (insetLeft | insetTop | insetRight | insetBottom) != 0;
    }
}
