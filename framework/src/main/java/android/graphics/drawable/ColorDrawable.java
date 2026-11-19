package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class ColorDrawable extends Drawable {
    private int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ColorDrawable() {
    }

    public ColorDrawable(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
        int effective = resolveTintedColor(color);
        if ((effective >>> 24) == 0) return;
        paint.setColor(effective);
        paint.setStyle(Paint.Style.FILL);
        Rect b = getBounds();
        canvas.drawRect(b, paint);
    }

    @Override
    public ConstantState getConstantState() {
        final int c = color;
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return new ColorDrawable(c);
            }
        };
    }
}
