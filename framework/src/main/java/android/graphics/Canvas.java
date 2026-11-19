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

    private record SavedState(AffineTransform transform, Shape clip, java.awt.Composite composite) {
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
            stack.push(new SavedState(g.getTransform(), g.getClip(), g.getComposite()));
        }
        return stack.size();
    }

    public int saveLayer(RectF bounds, Paint paint) {
        int count = save();
        if (g != null) {
            if (bounds != null) {
                clipRect(bounds);
            }
            if (paint != null && paint.getAlpha() < 255) {
                float a = Math.max(0f, Math.min(1f, paint.getAlpha() / 255f));
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, a));
            }
        }
        return count;
    }

    public int saveLayer(RectF bounds, Paint paint, int saveFlags) {
        return saveLayer(bounds, paint);
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
        return saveLayer(new RectF(left, top, right, bottom), paint);
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint, int saveFlags) {
        return saveLayer(left, top, right, bottom, paint);
    }

    public int saveLayerAlpha(RectF bounds, int alpha) {
        int count = save();
        if (g != null) {
            if (bounds != null) {
                clipRect(bounds);
            }
            float a = Math.max(0f, Math.min(1f, alpha / 255f));
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, a));
        }
        return count;
    }

    public int saveLayerAlpha(RectF bounds, int alpha, int saveFlags) {
        return saveLayerAlpha(bounds, alpha);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) {
        return saveLayerAlpha(new RectF(left, top, right, bottom), alpha);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha, int saveFlags) {
        return saveLayerAlpha(left, top, right, bottom, alpha);
    }

    public void restore() {
        if (g != null && !stack.isEmpty()) {
            SavedState s = stack.pop();
            g.setTransform(s.transform);
            g.setClip(s.clip);
            if (s.composite != null) {
                g.setComposite(s.composite);
            }
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
        return rect != null && clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipRect(RectF rect) {
        return rect != null && clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipOutRect(float left, float top, float right, float bottom) {
        if (g == null || right <= left || bottom <= top) return true;
        Shape cur = g.getClip();
        java.awt.geom.Area area = cur != null
                ? new java.awt.geom.Area(cur)
                : new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight())));
        area.subtract(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(left, top, right - left, bottom - top)));
        g.setClip(area);
        return !area.isEmpty();
    }

    public boolean clipOutRect(int left, int top, int right, int bottom) {
        return clipOutRect((float) left, (float) top, (float) right, (float) bottom);
    }

    public boolean clipOutRect(Rect rect) {
        return rect == null || clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipOutRect(RectF rect) {
        return rect == null || clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipPath(Path path) {
        if (g != null && path != null) {
            g.clip(path.awtPath);
        }
        return true;
    }

    public boolean clipOutPath(Path path) {
        if (g == null || path == null) return true;
        Shape cur = g.getClip();
        java.awt.geom.Area area = cur != null
                ? new java.awt.geom.Area(cur)
                : new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight())));
        area.subtract(new java.awt.geom.Area(path.awtPath));
        g.setClip(area);
        return !area.isEmpty();
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

    public static int applyColorFilter(int baseColor, ColorFilter filter) {
        if (!(filter instanceof PorterDuffColorFilter pdcf)) {
            return baseColor;
        }
        int fc = pdcf.getColor();
        PorterDuff.Mode mode = pdcf.getMode();
        if (mode == null) mode = PorterDuff.Mode.SRC_IN;
        int ba = Color.alpha(baseColor);
        int fa = Color.alpha(fc);
        return switch (mode) {
            case SRC -> fc;
            case SRC_IN -> Color.argb((ba * fa) / 255, Color.red(fc), Color.green(fc), Color.blue(fc));
            case SRC_ATOP -> Color.argb(ba, Color.red(fc), Color.green(fc), Color.blue(fc));
            case MULTIPLY -> Color.argb(
                    (ba * fa) / 255,
                    (Color.red(baseColor) * Color.red(fc)) / 255,
                    (Color.green(baseColor) * Color.green(fc)) / 255,
                    (Color.blue(baseColor) * Color.blue(fc)) / 255);
            case SRC_OVER -> {
                if (fa == 255) yield fc;
                if (fa == 0) yield baseColor;
                int inv = 255 - fa;
                int outA = fa + (ba * inv) / 255;
                if (outA == 0) yield 0;
                int outR = (Color.red(fc) * fa + Color.red(baseColor) * ba * inv / 255) / outA;
                int outG = (Color.green(fc) * fa + Color.green(baseColor) * ba * inv / 255) / outA;
                int outB = (Color.blue(fc) * fa + Color.blue(baseColor) * ba * inv / 255) / outA;
                yield Color.argb(Math.min(255, outA), Math.min(255, outR), Math.min(255, outG), Math.min(255, outB));
            }
            case CLEAR -> Color.TRANSPARENT;
            case DST -> baseColor;
            default -> Color.argb((ba * fa) / 255, Color.red(fc), Color.green(fc), Color.blue(fc));
        };
    }

    private void applyPaintToGraphics(Paint paint) {
        if (paint.getShader() instanceof LinearGradient lg) {
            g.setPaint(lg.toAwtPaint());
        } else {
            int effective = applyColorFilter(paint.getColor(), paint.getColorFilter());
            g.setColor(Color.toAwt(effective));
        }
    }

    private void drawShape(Shape shape, Paint paint) {
        if (g == null || paint == null) return;
        applyPaintToGraphics(paint);
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
        applyPaintToGraphics(paint);
        g.setStroke(paint.toAwtStroke());
        g.draw(new Line2D.Float(startX, startY, stopX, stopY));
    }

    public void drawLines(float[] pts, int offset, int count, Paint paint) {
        if (g == null || pts == null || paint == null) return;
        applyPaintToGraphics(paint);
        g.setStroke(paint.toAwtStroke());
        int end = Math.min(pts.length, offset + count);
        for (int i = Math.max(0, offset); i + 3 < end; i += 4) {
            g.draw(new Line2D.Float(pts[i], pts[i + 1], pts[i + 2], pts[i + 3]));
        }
    }

    public void drawLines(float[] pts, Paint paint) {
        if (pts != null) {
            drawLines(pts, 0, pts.length, paint);
        }
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
        g.setColor(Color.toAwt(applyColorFilter(paint.getColor(), paint.getColorFilter())));
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
