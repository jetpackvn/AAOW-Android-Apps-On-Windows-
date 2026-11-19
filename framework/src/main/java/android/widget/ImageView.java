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
    private boolean adjustViewBounds;
    private int maxWidth = Integer.MAX_VALUE;
    private int maxHeight = Integer.MAX_VALUE;

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
        if (context != null) {
            Resources res = context.getResources();
            if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
                XmlElement.Attribute src = xas.element.attr("src");
                if (src != null && src.value.isReference()) {
                    this.drawable = res.getDrawable(src.value.data(), context.getTheme());
                }
            }
            if (this.drawable == null && (defStyleAttr != 0 || defStyleRes != 0 || attrs != null)) {
                android.content.res.TypedArray a = context.obtainStyledAttributes(
                        attrs, new int[]{0x01010119}, defStyleAttr, defStyleRes); // android.R.attr.src
                Drawable d = a.getDrawable(0);
                a.recycle();
                if (d != null) {
                    this.drawable = d;
                }
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

    public boolean getAdjustViewBounds() {
        return adjustViewBounds;
    }

    public void setAdjustViewBounds(boolean adjustViewBounds) {
        this.adjustViewBounds = adjustViewBounds;
        requestLayout();
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        requestLayout();
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iw = drawable != null && drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 0;
        int ih = drawable != null && drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 0;
        int desiredW = Math.min(maxWidth, Math.max(getSuggestedMinimumWidth(), iw + paddingLeft + paddingRight));
        int desiredH = Math.min(maxHeight, Math.max(getSuggestedMinimumHeight(), ih + paddingTop + paddingBottom));
        setMeasuredDimension(resolveSize(desiredW, widthMeasureSpec), resolveSize(desiredH, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawable == null) return;
        int cw = getWidth() - paddingLeft - paddingRight;
        int ch = getHeight() - paddingTop - paddingBottom;
        if (cw <= 0 || ch <= 0) return;

        int iw = drawable.getIntrinsicWidth();
        int ih = drawable.getIntrinsicHeight();
        int dl = paddingLeft;
        int dt = paddingTop;
        int dw = cw;
        int dh = ch;
        if (iw > 0 && ih > 0 && scaleType != ScaleType.FIT_XY && scaleType != ScaleType.MATRIX) {
            if (scaleType == ScaleType.CENTER) {
                dw = iw;
                dh = ih;
                dl = paddingLeft + (cw - dw) / 2;
                dt = paddingTop + (ch - dh) / 2;
            } else if (scaleType == ScaleType.CENTER_CROP) {
                float s = Math.max((float) cw / iw, (float) ch / ih);
                dw = Math.round(iw * s);
                dh = Math.round(ih * s);
                dl = paddingLeft + (cw - dw) / 2;
                dt = paddingTop + (ch - dh) / 2;
            } else {
                float s = Math.min((float) cw / iw, (float) ch / ih);
                if (scaleType == ScaleType.CENTER_INSIDE && s > 1f) s = 1f;
                dw = Math.max(1, Math.round(iw * s));
                dh = Math.max(1, Math.round(ih * s));
                if (scaleType == ScaleType.FIT_START) {
                    dl = paddingLeft;
                    dt = paddingTop;
                } else if (scaleType == ScaleType.FIT_END) {
                    dl = paddingLeft + (cw - dw);
                    dt = paddingTop + (ch - dh);
                } else {
                    dl = paddingLeft + (cw - dw) / 2;
                    dt = paddingTop + (ch - dh) / 2;
                }
            }
        }
        drawable.setBounds(dl, dt, dl + dw, dt + dh);
        drawable.draw(canvas);
    }
}
