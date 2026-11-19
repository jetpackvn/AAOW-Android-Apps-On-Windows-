package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;

/** Hình elip/tròn dùng cho ShapeDrawable (đặc biệt trong CircleImageView của SwipeRefreshLayout). */
public class OvalShape extends RectShape {
    public OvalShape() {
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (canvas != null && paint != null) {
            canvas.drawOval(rect(), paint);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        if (outline != null) {
            RectF r = rect();
            outline.setOval(
                    Math.round(r.left),
                    Math.round(r.top),
                    Math.round(r.right),
                    Math.round(r.bottom)
            );
        }
    }
}
