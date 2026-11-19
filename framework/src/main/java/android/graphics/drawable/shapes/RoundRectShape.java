package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Hình chữ nhật bo góc dùng cho ShapeDrawable. */
public class RoundRectShape extends RectShape {
    private final float[] outerRadii;

    public RoundRectShape(float[] outerRadii, RectF inset, float[] innerRadii) {
        this.outerRadii = outerRadii != null ? outerRadii.clone() : null;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (canvas == null || paint == null) return;
        float rx = (outerRadii != null && outerRadii.length > 0) ? outerRadii[0] : 0f;
        float ry = (outerRadii != null && outerRadii.length > 1) ? outerRadii[1] : rx;
        canvas.drawRoundRect(rect(), rx, ry, paint);
    }
}
