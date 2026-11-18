package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import dev.aaow.apk.XmlElement;

public class ImageView extends View {
    public enum ScaleType {
        MATRIX,
        FIT_XY,
        FIT_START,
        FIT_CENTER,
        FIT_END,
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE
    }

    private Drawable drawable;
    private ScaleType scaleType = ScaleType.FIT_CENTER;
    private Matrix imageMatrix = new Matrix();
    private ColorStateList imageTintList;
    private PorterDuff.Mode imageTintMode;
    private boolean cropToPadding;

    public ImageView(Context context) {
        this(context, null, 0);
    }

    public ImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            Resources res = context.getResources();
            XmlElement.Attribute src = xas.element.attr("src");
            if (src != null && src.value.isReference()) {
                this.drawable = res.getDrawable(src.value.data());
            }
        }
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setImageDrawable(Drawable drawable) {
        this.drawable = drawable;
        requestLayout();
        invalidate();
    }

    public void setImageResource(int resId) {
        if (getResources() != null) {
            setImageDrawable(getResources().getDrawable(resId));
        }
    }

    public void setImageBitmap(Bitmap bm) {
        setImageDrawable(bm != null ? new BitmapDrawable(getResources(), bm) : null);
    }

    public void setScaleType(ScaleType scaleType) {
        if (scaleType != null) {
            this.scaleType = scaleType;
            invalidate();
        }
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public final void setColorFilter(int color) {
        invalidate();
    }

    public Matrix getImageMatrix() {
        return imageMatrix;
    }

    public void setImageMatrix(Matrix matrix) {
        this.imageMatrix = matrix != null ? new Matrix(matrix) : new Matrix();
        invalidate();
    }

    public ColorStateList getImageTintList() {
        return imageTintList;
    }

    public void setImageTintList(ColorStateList tint) {
        this.imageTintList = tint;
        if (drawable != null) drawable.setTintList(tint);
        invalidate();
    }

    public PorterDuff.Mode getImageTintMode() {
        return imageTintMode;
    }

    public void setImageTintMode(PorterDuff.Mode tintMode) {
        this.imageTintMode = tintMode;
        if (drawable != null) drawable.setTintMode(tintMode);
        invalidate();
    }

    public boolean getCropToPadding() {
        return cropToPadding;
    }

    public void setCropToPadding(boolean cropToPadding) {
        this.cropToPadding = cropToPadding;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iw = drawable != null && drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 48;
        int ih = drawable != null && drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 48;
        int desiredW = Math.max(getSuggestedMinimumWidth(), iw + paddingLeft + paddingRight);
        int desiredH = Math.max(getSuggestedMinimumHeight(), ih + paddingTop + paddingBottom);
        setMeasuredDimension(resolveSize(desiredW, widthMeasureSpec), resolveSize(desiredH, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawable == null) return;
        int cw = getWidth() - paddingLeft - paddingRight;
        int ch = getHeight() - paddingTop - paddingBottom;
        if (cw <= 0 || ch <= 0) return;

        drawable.setBounds(paddingLeft, paddingTop, paddingLeft + cw, paddingTop + ch);
        drawable.draw(canvas);
    }
}
