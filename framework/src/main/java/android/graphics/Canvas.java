package android.graphics;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cài đặt android.graphics.Canvas vẽ trực tiếp lên Graphics2D của Windows.
 */
public class Canvas {
    private Graphics2D g;
    private Bitmap targetBitmap;
    private final Deque<SavedState> stack = new ArrayDeque<>();

    private record SavedState(AffineTransform transform, Shape clip) {
    }

    public Canvas() {
    }

    public Canvas(Bitmap bitmap) {
        setBitmap(bitmap);
    }

    public Canvas(Graphics2D g) {
        this.g = g;
        enableHints(this.g);
    }

    public void setBitmap(Bitmap bitmap) {
        this.targetBitmap = bitmap;
        if (bitmap != null) {
            this.g = bitmap.getAwtImage().createGraphics();
            enableHints(this.g);
        }
    }

    private static void enableHints(Graphics2D g) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public Graphics2D getAwtGraphics() {
        return g;
    }

    public int getWidth() {
        if (targetBitmap != null) return targetBitmap.getWidth();
        java.awt.Rectangle clip = g != null ? g.getClipBounds() : null;
        return clip != null ? clip.width : 0;
    }

    public int getHeight() {
        if (targetBitmap != null) return targetBitmap.getHeight();
        java.awt.Rectangle clip = g != null ? g.getClipBounds() : null;
        return clip != null ? clip.height : 0;
    }

    public int save() {
        if (g != null) {
            stack.push(new SavedState(g.getTransform(), g.getClip()));
        }
        return stack.size();
    }

    public void restore() {
        if (g != null && !stack.isEmpty()) {
            SavedState s = stack.pop();
            g.setTransform(s.transform);
            g.setClip(s.clip);
        }
    }

    public void restoreToCount(int saveCount) {
        while (stack.size() >= saveCount && !stack.isEmpty()) {
            restore();
        }
    }

    public int getSaveCount() {
        return stack.size() + 1;
    }

    public void translate(float dx, float dy) {
        if (g != null) g.translate(dx, dy);
    }

    public void scale(float sx, float sy) {
        if (g != null) g.scale(sx, sy);
    }

    public final void scale(float sx, float sy, float px, float py) {
        translate(px, py);
        scale(sx, sy);
        translate(-px, -py);
    }

    public void rotate(float degrees) {
        if (g != null) g.rotate(Math.toRadians(degrees));
    }

    public final void rotate(float degrees, float px, float py) {
        translate(px, py);
        rotate(degrees);
        translate(-px, -py);
    }

    public void concat(Matrix matrix) {
        if (g != null && matrix != null) {
            g.transform(matrix.getAwtTransform());
        }
    }

    public boolean clipRect(int left, int top, int right, int bottom) {
        if (g != null) g.clipRect(left, top, right - left, bottom - top);
        return true;
    }

    public boolean clipRect(float left, float top, float right, float bottom) {
        return clipRect(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }

    public boolean clipRect(Rect rect) {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipPath(Path path) {
        if (g != null && path != null) {
            g.clip(path.awtPath);
        }
        return true;
    }

    public void drawColor(int color) {
        if (g == null) return;
        g.setColor(Color.toAwt(color));
        java.awt.Rectangle b = g.getClipBounds();
        if (b != null) {
            g.fillRect(b.x, b.y, b.width, b.height);
        } else {
            g.fillRect(0, 0, 2000, 2000);
        }
    }

    public void drawARGB(int a, int r, int g, int b) {
        drawColor(Color.argb(a, r, g, b));
    }

    public void drawRGB(int r, int g, int b) {
        drawColor(Color.rgb(r, g, b));
    }

    private void drawShape(Shape shape, Paint paint) {
        if (g == null || paint == null) return;
        g.setColor(Color.toAwt(paint.getColor()));
        Paint.Style style = paint.getStyle();
        if (style == Paint.Style.FILL || style == Paint.Style.FILL_AND_STROKE) {
            g.fill(shape);
        }
        if (style == Paint.Style.STROKE || style == Paint.Style.FILL_AND_STROKE) {
            g.setStroke(paint.toAwtStroke());
            g.draw(shape);
        }
    }

    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        if (g == null || paint == null) return;
        g.setColor(Color.toAwt(paint.getColor()));
        g.setStroke(paint.toAwtStroke());
        g.draw(new Line2D.Float(startX, startY, stopX, stopY));
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        drawShape(new Rectangle2D.Float(left, top, right - left, bottom - top), paint);
    }

    public void drawRect(Rect r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    public void drawRect(RectF rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
        drawShape(new RoundRectangle2D.Float(left, top, right - left, bottom - top, rx * 2, ry * 2), paint);
    }

    public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint);
    }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        drawShape(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2), paint);
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint) {
        drawShape(new Ellipse2D.Float(left, top, right - left, bottom - top), paint);
    }

    public void drawOval(RectF oval, Paint paint) {
        drawOval(oval.left, oval.top, oval.right, oval.bottom, paint);
    }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        int type = useCenter ? Arc2D.PIE : Arc2D.OPEN;
        drawShape(new Arc2D.Float(left, top, right - left, bottom - top, -startAngle, -sweepAngle, type), paint);
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint);
    }

    public void drawPath(Path path, Paint paint) {
        if (path != null) drawShape(path.awtPath, paint);
    }

    public void drawText(String text, float x, float y, Paint paint) {
        if (g == null || text == null || paint == null) return;
        Font font = paint.toAwtFont();
        g.setFont(font);
        g.setColor(Color.toAwt(paint.getColor()));
        FontMetrics fm = g.getFontMetrics(font);
        float drawX = switch (paint.getTextAlign()) {
            case CENTER -> x - fm.stringWidth(text) / 2f;
            case RIGHT -> x - fm.stringWidth(text);
            default -> x;
        };
        g.drawString(text, drawX, y);
    }

    public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint) {
        if (text != null) drawText(text.subSequence(start, end).toString(), x, y, paint);
    }

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        if (g == null || bitmap == null || bitmap.isRecycled()) return;
        java.awt.image.BufferedImage awt = bitmap.getAwtImage();
        if (awt == null) return;
        java.awt.Composite oldComp = null;
        if (paint != null && paint.getAlpha() < 255) {
            oldComp = g.getComposite();
            g.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, paint.getAlpha() / 255f))));
        }
        g.drawImage(awt, Math.round(left), Math.round(top), null);
        if (oldComp != null) g.setComposite(oldComp);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
        if (g == null || bitmap == null || dst == null || bitmap.isRecycled()) return;
        java.awt.image.BufferedImage awt = bitmap.getAwtImage();
        if (awt == null) return;
        java.awt.Composite oldComp = null;
        if (paint != null && paint.getAlpha() < 255) {
            oldComp = g.getComposite();
            g.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, paint.getAlpha() / 255f))));
        }
        Object prevInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (src == null) {
            g.drawImage(awt, dst.left, dst.top, dst.width(), dst.height(), null);
        } else {
            g.drawImage(awt, dst.left, dst.top, dst.right, dst.bottom,
                    src.left, src.top, src.right, src.bottom, null);
        }
        if (prevInterp != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterp);
        }
        if (oldComp != null) g.setComposite(oldComp);
    }
}
