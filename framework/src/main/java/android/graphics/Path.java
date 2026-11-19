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

    public enum Op {
        DIFFERENCE,
        INTERSECT,
        UNION,
        XOR,
        REVERSE_DIFFERENCE
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

    public void rMoveTo(float dx, float dy) {
        java.awt.geom.Point2D pt = awtPath.getCurrentPoint();
        if (pt == null) awtPath.moveTo(dx, dy);
        else awtPath.moveTo(pt.getX() + dx, pt.getY() + dy);
    }

    public void lineTo(float x, float y) {
        if (awtPath.getCurrentPoint() == null) awtPath.moveTo(0, 0);
        awtPath.lineTo(x, y);
    }

    public void rLineTo(float dx, float dy) {
        java.awt.geom.Point2D pt = awtPath.getCurrentPoint();
        if (pt == null) {
            awtPath.moveTo(0, 0);
            awtPath.lineTo(dx, dy);
        } else {
            awtPath.lineTo(pt.getX() + dx, pt.getY() + dy);
        }
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        if (awtPath.getCurrentPoint() == null) awtPath.moveTo(0, 0);
        awtPath.quadTo(x1, y1, x2, y2);
    }

    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        java.awt.geom.Point2D pt = awtPath.getCurrentPoint();
        float cx = pt != null ? (float) pt.getX() : 0f;
        float cy = pt != null ? (float) pt.getY() : 0f;
        if (pt == null) awtPath.moveTo(0, 0);
        awtPath.quadTo(cx + dx1, cy + dy1, cx + dx2, cy + dy2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (awtPath.getCurrentPoint() == null) awtPath.moveTo(0, 0);
        awtPath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void rCubicTo(float dx1, float dy1, float dx2, float dy2, float dx3, float dy3) {
        java.awt.geom.Point2D pt = awtPath.getCurrentPoint();
        float cx = pt != null ? (float) pt.getX() : 0f;
        float cy = pt != null ? (float) pt.getY() : 0f;
        if (pt == null) awtPath.moveTo(0, 0);
        awtPath.curveTo(cx + dx1, cy + dy1, cx + dx2, cy + dy2, cx + dx3, cy + dy3);
    }

    public void close() {
        if (awtPath.getCurrentPoint() != null) {
            awtPath.closePath();
        }
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

    public void addOval(RectF oval, Direction dir) {
        if (oval != null) {
            addOval(oval.left, oval.top, oval.right, oval.bottom, dir);
        }
    }

    public void addOval(float left, float top, float right, float bottom, Direction dir) {
        awtPath.append(new Ellipse2D.Float(left, top, right - left, bottom - top), false);
    }

    public void addPath(Path src) {
        if (src != null) {
            awtPath.append(src.awtPath, false);
        }
    }

    public void addPath(Path src, float dx, float dy) {
        if (src != null) {
            Path copy = new Path(src);
            Matrix m = new Matrix();
            m.setTranslate(dx, dy);
            copy.transform(m);
            awtPath.append(copy.awtPath, false);
        }
    }

    public void addPath(Path src, Matrix matrix) {
        if (src != null) {
            Path copy = new Path(src);
            copy.transform(matrix);
            awtPath.append(copy.awtPath, false);
        }
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        awtPath.append(new Arc2D.Float(oval.left, oval.top, oval.width(), oval.height(), -startAngle, -sweepAngle, Arc2D.OPEN), true);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
        awtPath.append(new Arc2D.Float(oval.left, oval.top, oval.width(), oval.height(), -startAngle, -sweepAngle, Arc2D.OPEN), !forceMoveTo);
    }

    public void set(Path src) {
        awtPath.reset();
        if (src != null) {
            awtPath.append(src.awtPath, false);
        }
    }

    public void transform(Matrix matrix) {
        if (matrix != null) {
            awtPath.transform(matrix.getAwtTransform());
        }
    }

    public void transform(Matrix matrix, Path dst) {
        Path target = dst != null ? dst : this;
        if (target != this) target.set(this);
        if (matrix != null) target.awtPath.transform(matrix.getAwtTransform());
    }

    public void setFillType(FillType ft) {
        if (ft == FillType.EVEN_ODD || ft == FillType.INVERSE_EVEN_ODD) {
            awtPath.setWindingRule(Path2D.WIND_EVEN_ODD);
        } else {
            awtPath.setWindingRule(Path2D.WIND_NON_ZERO);
        }
    }

    public void computeBounds(RectF bounds, boolean exact) {
        if (bounds == null) return;
        Rectangle2D r = awtPath.getBounds2D();
        bounds.set((float) r.getMinX(), (float) r.getMinY(), (float) r.getMaxX(), (float) r.getMaxY());
    }

    public boolean isEmpty() {
        return awtPath.getCurrentPoint() == null;
    }

    public boolean isConvex() {
        return true;
    }

    public boolean isRect(RectF rect) {
        return false;
    }

    public boolean op(Path path, Op op) {
        return op(this, path, op);
    }

    public boolean op(Path path1, Path path2, Op op) {
        if (path1 == null || path2 == null || op == null) return false;
        java.awt.geom.Area a = new java.awt.geom.Area(path1.awtPath);
        java.awt.geom.Area b = new java.awt.geom.Area(path2.awtPath);
        switch (op) {
            case DIFFERENCE -> a.subtract(b);
            case INTERSECT -> a.intersect(b);
            case UNION -> a.add(b);
            case XOR -> a.exclusiveOr(b);
            case REVERSE_DIFFERENCE -> {
                b.subtract(a);
                a = b;
            }
        }
        awtPath.reset();
        awtPath.append(a, false);
        return !awtPath.getBounds2D().isEmpty();
    }
}
