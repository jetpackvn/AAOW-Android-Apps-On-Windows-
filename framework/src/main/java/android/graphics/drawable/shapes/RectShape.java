package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;

/** Hình chữ nhật dùng cho ShapeDrawable và làm lớp cha của OvalShape / RoundRectShape. */
public class RectShape extends Shape {
    private RectF rect = new RectF();

    public RectShape() {
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (canvas != null && paint != null) {
            canvas.drawRect(rect, paint);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        if (outline != null) {
            outline.setRect(
                    Math.round(rect.left),
                    Math.round(rect.top),
                    Math.round(rect.right),
                    Math.round(rect.bottom)
            );
        }
    }

    @Override
    protected void onResize(float width, float height) {
        rect.set(0f, 0f, width, height);
    }

    protected final RectF rect() {
        return rect;
    }

    @Override
    public RectShape clone() throws CloneNotSupportedException {
        final RectShape shape = (RectShape) super.clone();
        shape.rect = new RectF(rect);
        return shape;
    }
}
