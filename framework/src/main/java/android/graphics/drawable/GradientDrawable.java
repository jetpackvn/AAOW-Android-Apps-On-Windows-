package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

public class GradientDrawable extends Drawable {
    public static final int RECTANGLE = 0;
    public static final int OVAL = 1;
    public static final int LINE = 2;
    public static final int RING = 3;

    public enum Orientation {
        TOP_BOTTOM,
        TR_BL,
        RIGHT_LEFT,
        BR_TL,
        BOTTOM_TOP,
        BL_TR,
        LEFT_RIGHT,
        TL_BR
    }

    private int shape = RECTANGLE;
    private Orientation orientation = Orientation.TOP_BOTTOM;
    private int[] colors;
    private int solidColor = Color.TRANSPARENT;
    private float cornerRadius;
    private int strokeWidth;
    private int strokeColor = Color.TRANSPARENT;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GradientDrawable() {
    }

    public GradientDrawable(Orientation orientation, int[] colors) {
        this.orientation = orientation;
        this.colors = colors;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public void setColor(int argb) {
        this.solidColor = argb;
        this.colors = null;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = Math.max(0f, radius);
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setStroke(int width, int color) {
        this.strokeWidth = width;
        this.strokeColor = color;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        Graphics2D g = canvas.getAwtGraphics();
        if (g != null && colors != null && colors.length >= 2) {
            float x1 = b.left, y1 = b.top, x2 = b.left, y2 = b.bottom;
            if (orientation == Orientation.LEFT_RIGHT) {
                x2 = b.right;
                y2 = b.top;
            } else if (orientation == Orientation.TL_BR) {
                x2 = b.right;
                y2 = b.bottom;
            } else if (orientation == Orientation.BL_TR) {
                y1 = b.bottom;
                x2 = b.right;
                y2 = b.top;
            }
            g.setPaint(new GradientPaint(x1, y1, Color.toAwt(colors[0]), x2, y2, Color.toAwt(colors[colors.length - 1])));
            g.fill(new RoundRectangle2D.Float(b.left, b.top, b.width(), b.height(), cornerRadius * 2, cornerRadius * 2));
        } else if ((solidColor >>> 24) != 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(solidColor);
            if (shape == OVAL) {
                canvas.drawOval(b.left, b.top, b.right, b.bottom, paint);
            } else {
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, cornerRadius, cornerRadius, paint);
            }
        }
        if (strokeWidth > 0 && (strokeColor >>> 24) != 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(strokeColor);
            float inset = strokeWidth / 2f;
            if (shape == OVAL) {
                canvas.drawOval(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset, paint);
            } else {
                canvas.drawRoundRect(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset, cornerRadius, cornerRadius, paint);
            }
        }
    }
}
