package android.graphics;

import java.awt.geom.AffineTransform;

/**
 * Cài đặt android.graphics.Matrix dựa trên AffineTransform của Java2D.
 */
public class Matrix {
    public static final int MSCALE_X = 0;
    public static final int MSKEW_X = 1;
    public static final int MTRANS_X = 2;
    public static final int MSKEW_Y = 3;
    public static final int MSCALE_Y = 4;
    public static final int MTRANS_Y = 5;
    public static final int MPERSP_0 = 6;
    public static final int MPERSP_1 = 7;
    public static final int MPERSP_2 = 8;

    private final AffineTransform tx = new AffineTransform();

    public Matrix() {
    }

    public Matrix(Matrix src) {
        if (src != null) tx.setTransform(src.tx);
    }

    public AffineTransform getAwtTransform() {
        return tx;
    }

    public boolean isIdentity() {
        return tx.isIdentity();
    }

    public void reset() {
        tx.setToIdentity();
    }

    public void set(Matrix src) {
        if (src == null) reset();
        else tx.setTransform(src.tx);
    }

    public void setTranslate(float dx, float dy) {
        tx.setToTranslation(dx, dy);
    }

    public void setScale(float sx, float sy) {
        tx.setToScale(sx, sy);
    }

    public void setScale(float sx, float sy, float px, float py) {
        tx.setToTranslation(px, py);
        tx.scale(sx, sy);
        tx.translate(-px, -py);
    }

    public void setRotate(float degrees) {
        tx.setToRotation(Math.toRadians(degrees));
    }

    public void setRotate(float degrees, float px, float py) {
        tx.setToRotation(Math.toRadians(degrees), px, py);
    }

    public boolean preTranslate(float dx, float dy) {
        tx.translate(dx, dy);
        return true;
    }

    public boolean preScale(float sx, float sy) {
        tx.scale(sx, sy);
        return true;
    }

    public boolean preScale(float sx, float sy, float px, float py) {
        tx.translate(px, py);
        tx.scale(sx, sy);
        tx.translate(-px, -py);
        return true;
    }

    public boolean preRotate(float degrees) {
        tx.rotate(Math.toRadians(degrees));
        return true;
    }

    public boolean preRotate(float degrees, float px, float py) {
        tx.rotate(Math.toRadians(degrees), px, py);
        return true;
    }

    public boolean postTranslate(float dx, float dy) {
        AffineTransform op = AffineTransform.getTranslateInstance(dx, dy);
        op.concatenate(tx);
        tx.setTransform(op);
        return true;
    }

    public boolean postScale(float sx, float sy) {
        AffineTransform op = AffineTransform.getScaleInstance(sx, sy);
        op.concatenate(tx);
        tx.setTransform(op);
        return true;
    }

    public boolean postScale(float sx, float sy, float px, float py) {
        AffineTransform op = AffineTransform.getTranslateInstance(px, py);
        op.scale(sx, sy);
        op.translate(-px, -py);
        op.concatenate(tx);
        tx.setTransform(op);
        return true;
    }

    public boolean postRotate(float degrees) {
        AffineTransform op = AffineTransform.getRotateInstance(Math.toRadians(degrees));
        op.concatenate(tx);
        tx.setTransform(op);
        return true;
    }

    public boolean postRotate(float degrees, float px, float py) {
        AffineTransform op = AffineTransform.getRotateInstance(Math.toRadians(degrees), px, py);
        op.concatenate(tx);
        tx.setTransform(op);
        return true;
    }

    public boolean preConcat(Matrix other) {
        if (other != null) tx.concatenate(other.tx);
        return true;
    }

    public boolean postConcat(Matrix other) {
        if (other != null) {
            AffineTransform op = new AffineTransform(other.tx);
            op.concatenate(tx);
            tx.setTransform(op);
        }
        return true;
    }

    public boolean invert(Matrix inverse) {
        try {
            AffineTransform inv = tx.createInverse();
            if (inverse != null) inverse.tx.setTransform(inv);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void mapPoints(float[] pts) {
        if (pts != null) mapPoints(pts, 0, pts, 0, pts.length / 2);
    }

    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex, int pointCount) {
        tx.transform(src, srcIndex, dst, dstIndex, pointCount);
    }

    public boolean mapRect(RectF rect) {
        return mapRect(rect, rect);
    }

    public boolean mapRect(RectF dst, RectF src) {
        if (dst == null || src == null) return false;
        float[] pts = {src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom};
        mapPoints(pts);
        float minX = Math.min(Math.min(pts[0], pts[2]), Math.min(pts[4], pts[6]));
        float maxX = Math.max(Math.max(pts[0], pts[2]), Math.max(pts[4], pts[6]));
        float minY = Math.min(Math.min(pts[1], pts[3]), Math.min(pts[5], pts[7]));
        float maxY = Math.max(Math.max(pts[1], pts[3]), Math.max(pts[5], pts[7]));
        dst.set(minX, minY, maxX, maxY);
        return true;
    }

    public void getValues(float[] values) {
        if (values == null || values.length < 9) return;
        values[MSCALE_X] = (float) tx.getScaleX();
        values[MSKEW_X] = (float) tx.getShearX();
        values[MTRANS_X] = (float) tx.getTranslateX();
        values[MSKEW_Y] = (float) tx.getShearY();
        values[MSCALE_Y] = (float) tx.getScaleY();
        values[MTRANS_Y] = (float) tx.getTranslateY();
        values[MPERSP_0] = 0f;
        values[MPERSP_1] = 0f;
        values[MPERSP_2] = 1f;
    }

    public void setValues(float[] values) {
        if (values == null || values.length < 9) return;
        tx.setTransform(values[MSCALE_X], values[MSKEW_Y], values[MSKEW_X],
                values[MSCALE_Y], values[MTRANS_X], values[MTRANS_Y]);
    }
}
