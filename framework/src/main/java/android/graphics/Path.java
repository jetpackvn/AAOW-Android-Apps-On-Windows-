package android.graphics;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class Path {
    public enum Direction {
        CW,
        CCW
    }

    public enum FillType {
        WINDING,
        EVEN_ODD,
        INVERSE_WINDING,
        INVERSE_EVEN_ODD
    }

    final Path2D.Float awtPath = new Path2D.Float();

    public Path() {
    }

    public Path(Path src) {
        if (src != null) awtPath.append(src.awtPath, false);
    }

    public void reset() {
        awtPath.reset();
    }

    public void rewind() {
        awtPath.reset();
    }

    public void moveTo(float x, float y) {
        awtPath.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        awtPath.lineTo(x, y);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        awtPath.quadTo(x1, y1, x2, y2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        awtPath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void close() {
        awtPath.closePath();
    }

    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        awtPath.append(new Rectangle2D.Float(left, top, right - left, bottom - top), false);
    }

    public void addRect(RectF rect, Direction dir) {
        addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }

    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        awtPath.append(new RoundRectangle2D.Float(rect.left, rect.top, rect.width(), rect.height(), rx * 2, ry * 2), false);
    }

    public void addCircle(float x, float y, float radius, Direction dir) {
        awtPath.append(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2), false);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        awtPath.append(new Arc2D.Float(oval.left, oval.top, oval.width(), oval.height(), -startAngle, -sweepAngle, Arc2D.OPEN), true);
    }
}
