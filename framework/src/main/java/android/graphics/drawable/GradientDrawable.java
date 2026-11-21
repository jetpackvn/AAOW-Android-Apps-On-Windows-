package android.graphics.drawable;

import android.content.res.ColorStateList;
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

    public static final int LINEAR_GRADIENT = 0;
    public static final int RADIAL_GRADIENT = 1;
    public static final int SWEEP_GRADIENT = 2;

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
    private int gradientType = LINEAR_GRADIENT;
    private Orientation orientation = Orientation.TOP_BOTTOM;
    private int[] colors;
    private ColorStateList solidColorList;
    private int solidColor = Color.TRANSPARENT;
    private float cornerRadius;
    private float[] cornerRadii;
    private int strokeWidth;
    private int strokeColor = Color.TRANSPARENT;
    private ColorStateList strokeColorList;
    private float gradientCenterX = 0.5f;
    private float gradientCenterY = 0.5f;
    private float gradientRadius = 0.5f;
    private boolean useLevel;
    private int intrinsicW = -1;
    private int intrinsicH = -1;
    private Rect padding;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GradientDrawable() {
    }

    public GradientDrawable(Orientation orientation, int[] colors) {
        this.orientation = orientation != null ? orientation : Orientation.TOP_BOTTOM;
        this.colors = colors;
    }

    public void setShape(int shape) {
        this.shape = shape;
        invalidateSelf();
    }

    public int getShape() {
        return shape;
    }

    public void setGradientType(int gradient) {
        this.gradientType = gradient;
        invalidateSelf();
    }

    public int getGradientType() {
        return gradientType;
    }

    public void setGradientCenter(float x, float y) {
        this.gradientCenterX = x;
        this.gradientCenterY = y;
        invalidateSelf();
    }

    public float getGradientCenterX() {
        return gradientCenterX;
    }

    public float getGradientCenterY() {
        return gradientCenterY;
    }

    public void setGradientRadius(float gradientRadius) {
        this.gradientRadius = gradientRadius;
        invalidateSelf();
    }

    public float getGradientRadius() {
        return gradientRadius;
    }

    public void setUseLevel(boolean useLevel) {
        this.useLevel = useLevel;
        invalidateSelf();
    }

    public boolean getUseLevel() {
        return useLevel;
    }

    public void setColor(int argb) {
        this.solidColor = argb;
        this.solidColorList = ColorStateList.valueOf(argb);
        this.colors = null;
        invalidateSelf();
    }

    public void setColor(ColorStateList colorStateList) {
        this.solidColorList = colorStateList;
        this.solidColor = colorStateList != null ? colorStateList.getColorForState(getState(), colorStateList.getDefaultColor()) : Color.TRANSPARENT;
        this.colors = null;
        invalidateSelf();
    }

    public ColorStateList getColor() {
        return solidColorList != null ? solidColorList : ColorStateList.valueOf(solidColor);
    }

    public void setColors(int[] colors) {
        this.colors = colors;
        invalidateSelf();
    }

    public void setColors(int[] colors, float[] offsets) {
        setColors(colors);
    }

    public int[] getColors() {
        return colors != null ? colors.clone() : null;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation != null ? orientation : Orientation.TOP_BOTTOM;
        invalidateSelf();
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = Math.max(0f, radius);
        this.cornerRadii = null;
        invalidateSelf();
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadii(float[] radii) {
        this.cornerRadii = radii != null ? radii.clone() : null;
        if (radii != null && radii.length > 0) {
            float sum = 0f;
            for (float r : radii) sum += Math.max(0f, r);
            this.cornerRadius = sum / radii.length;
        }
        invalidateSelf();
    }

    public float[] getCornerRadii() {
        return cornerRadii != null ? cornerRadii.clone() : null;
    }

    public void setStroke(int width, int color) {
        setStroke(width, color, 0f, 0f);
    }

    public void setStroke(int width, int color, float dashWidth, float dashGap) {
        this.strokeWidth = width;
        this.strokeColor = color;
        this.strokeColorList = ColorStateList.valueOf(color);
        invalidateSelf();
    }

    public void setStroke(int width, ColorStateList colorStateList) {
        setStroke(width, colorStateList, 0f, 0f);
    }

    public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
        this.strokeWidth = width;
        this.strokeColorList = colorStateList;
        this.strokeColor = colorStateList != null ? colorStateList.getColorForState(getState(), colorStateList.getDefaultColor()) : Color.TRANSPARENT;
        invalidateSelf();
    }

    public void setSize(int width, int height) {
        this.intrinsicW = width;
        this.intrinsicH = height;
        invalidateSelf();
    }

    public void setPadding(int left, int top, int right, int bottom) {
        this.padding = new Rect(left, top, right, bottom);
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
    public int getIntrinsicWidth() {
        return intrinsicW;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicH;
    }

    public void setDither(boolean dither) {
        paint.setDither(dither);
    }

    @Override
    public Drawable mutate() {
        return this;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean changed = false;
        if (solidColorList != null && solidColorList.isStateful()) {
            int next = solidColorList.getColorForState(stateSet, solidColorList.getDefaultColor());
            if (next != solidColor) {
                solidColor = next;
                changed = true;
            }
        }
        if (strokeColorList != null && strokeColorList.isStateful()) {
            int next = strokeColorList.getColorForState(stateSet, strokeColorList.getDefaultColor());
            if (next != strokeColor) {
                strokeColor = next;
                changed = true;
            }
        }
        if (changed) invalidateSelf();
        return changed;
    }

    @Override
    public boolean isStateful() {
        return (solidColorList != null && solidColorList.isStateful())
                || (strokeColorList != null && strokeColorList.isStateful())
                || super.isStateful();
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
            } else if (orientation == Orientation.RIGHT_LEFT) {
                x1 = b.right;
                x2 = b.left;
                y2 = b.top;
            } else if (orientation == Orientation.BOTTOM_TOP) {
                y1 = b.bottom;
                y2 = b.top;
            } else if (orientation == Orientation.TL_BR) {
                x2 = b.right;
                y2 = b.bottom;
            } else if (orientation == Orientation.BL_TR) {
                y1 = b.bottom;
                x2 = b.right;
                y2 = b.top;
            } else if (orientation == Orientation.TR_BL) {
                x1 = b.right;
                x2 = b.left;
                y2 = b.bottom;
            } else if (orientation == Orientation.BR_TL) {
                x1 = b.right;
                y1 = b.bottom;
                x2 = b.left;
                y2 = b.top;
            }
            g.setPaint(new GradientPaint(x1, y1, Color.toAwt(colors[0]), x2, y2, Color.toAwt(colors[colors.length - 1])));
            g.fill(new RoundRectangle2D.Float(b.left, b.top, b.width(), b.height(), cornerRadius * 2, cornerRadius * 2));
        } else {
            int effectiveSolid = (solidColor >>> 24) != 0 ? resolveTintedColor(solidColor)
                    : (getTintListInternal() != null ? resolveTintedColor(0xFFFFFFFF) : 0);
            if ((effectiveSolid >>> 24) != 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(effectiveSolid);
                if (shape == OVAL) {
                    canvas.drawOval(b.left, b.top, b.right, b.bottom, paint);
                } else {
                    canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, cornerRadius, cornerRadius, paint);
                }
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

    @Override
    public ConstantState getConstantState() {
        final int fShape = shape;
        final int fGradType = gradientType;
        final Orientation fOrientation = orientation;
        final int[] fColors = colors != null ? colors.clone() : null;
        final int fSolid = solidColor;
        final ColorStateList fSolidList = solidColorList;
        final float fCorner = cornerRadius;
        final float[] fCornerRadii = cornerRadii != null ? cornerRadii.clone() : null;
        final int fStrokeW = strokeWidth;
        final int fStrokeCol = strokeColor;
        final ColorStateList fStrokeList = strokeColorList;
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                GradientDrawable copy = new GradientDrawable();
                copy.shape = fShape;
                copy.gradientType = fGradType;
                copy.orientation = fOrientation;
                copy.colors = fColors != null ? fColors.clone() : null;
                copy.solidColor = fSolid;
                copy.solidColorList = fSolidList;
                copy.cornerRadius = fCorner;
                copy.cornerRadii = fCornerRadii != null ? fCornerRadii.clone() : null;
                copy.strokeWidth = fStrokeW;
                copy.strokeColor = fStrokeCol;
                copy.strokeColorList = fStrokeList;
                return copy;
            }
        };
    }
}
