package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.shapes.Shape;

/**
 * Cài đặt thật android.graphics.drawable.ShapeDrawable (vẽ Shape bằng Paint, dùng trong SwipeRefreshLayout).
 */
public class ShapeDrawable extends Drawable {
    public abstract static class ShaderFactory {
        public abstract Shader resize(int width, int height);
    }

    private Shape shape;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int intrinsicWidth;
    private int intrinsicHeight;
    private int alpha = 255;
    private Rect padding;
    private ShaderFactory shaderFactory;

    public ShapeDrawable() {
    }

    public ShapeDrawable(Shape s) {
        this.shape = s;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape s) {
        this.shape = s;
        updateShape();
    }

    public void setShaderFactory(ShaderFactory fact) {
        this.shaderFactory = fact;
    }

    public ShaderFactory getShaderFactory() {
        return shaderFactory;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setIntrinsicWidth(int width) {
        this.intrinsicWidth = width;
    }

    public void setIntrinsicHeight(int height) {
        this.intrinsicHeight = height;
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        if ((left | top | right | bottom) == 0) {
            padding = null;
        } else {
            if (padding == null) padding = new Rect();
            padding.set(left, top, right, bottom);
        }
    }

    public void setPadding(Rect padding) {
        if (padding == null) {
            this.padding = null;
        } else {
            if (this.padding == null) this.padding = new Rect();
            this.padding.set(padding);
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.padding != null && padding != null) {
            padding.set(this.padding);
            return true;
        }
        return super.getPadding(padding);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateShape();
    }

    private void updateShape() {
        if (shape != null) {
            Rect r = getBounds();
            int w = Math.max(0, r.width());
            int h = Math.max(0, r.height());
            shape.resize(w, h);
            if (shaderFactory != null) {
                paint.setShader(shaderFactory.resize(w, h));
            }
        }
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (canvas == null || shape == null) return;
        Rect r = getBounds();
        if (r.isEmpty()) return;
        int prevAlpha = paint.getAlpha();
        paint.setAlpha((prevAlpha * alpha) / 255);
        int count = canvas.save();
        canvas.translate(r.left, r.top);
        shape.resize(r.width(), r.height());
        shape.draw(canvas, paint);
        canvas.restoreToCount(count);
        paint.setAlpha(prevAlpha);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void getOutline(Outline outline) {
        if (shape != null && outline != null) {
            shape.getOutline(outline);
            outline.setAlpha(getAlpha() / 255.0f);
        }
    }
}
