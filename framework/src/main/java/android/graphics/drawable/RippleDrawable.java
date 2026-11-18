package android.graphics.drawable;

import android.content.res.ColorStateList;

public class RippleDrawable extends LayerDrawable {
    public static final int RADIUS_AUTO = -1;

    private ColorStateList color;
    private int radius = RADIUS_AUTO;

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask) {
        super(content != null ? new Drawable[]{content} : new Drawable[0]);
        this.color = color;
    }

    public void setColor(ColorStateList color) {
        this.color = color;
        invalidateSelf();
    }

    public ColorStateList getColor() {
        return color;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }
}
