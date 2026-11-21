package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public class ProgressBar extends View {
    protected int min = 0;
    protected int max = 100;
    protected int progress = 0;
    protected int secondaryProgress = 0;
    protected boolean indeterminate = false;
    protected Drawable progressDrawable;
    protected Drawable indeterminateDrawable;
    protected ColorStateList progressTintList;
    protected PorterDuff.Mode progressTintMode;
    protected ColorStateList progressBackgroundTintList;
    protected ColorStateList indeterminateTintList;
    protected final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ProgressBar(Context context) {
        this(context, null, 0);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMinimumHeight(20);
        setMinimumWidth(100);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            Resources res = context.getResources();
            XmlElement.Attribute m = xas.element.attr("max");
            if (m != null) {
                ResValue rv = res.getTable().resolve(m.value);
                if (rv != null && rv.data() > 0) this.max = rv.data();
            }
            XmlElement.Attribute p = xas.element.attr("progress");
            if (p != null) {
                ResValue rv = res.getTable().resolve(p.value);
                if (rv != null) this.progress = Math.max(0, Math.min(this.max, rv.data()));
            }
        }
    }

    public synchronized int getMin() {
        return min;
    }

    public synchronized void setMin(int min) {
        this.min = min;
        if (progress < this.min) progress = this.min;
        invalidate();
    }

    public synchronized int getMax() {
        return max;
    }

    public synchronized void setMax(int max) {
        this.max = Math.max(1, max);
        if (progress > this.max) progress = this.max;
        if (secondaryProgress > this.max) secondaryProgress = this.max;
        invalidate();
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        setProgressInternal(progress, false);
    }

    public synchronized void setProgress(int progress, boolean animate) {
        setProgressInternal(progress, false);
    }

    public synchronized int getSecondaryProgress() {
        return secondaryProgress;
    }

    public synchronized void setSecondaryProgress(int secondaryProgress) {
        int clamped = Math.max(min, Math.min(max, secondaryProgress));
        if (this.secondaryProgress != clamped) {
            this.secondaryProgress = clamped;
            invalidate();
        }
    }

    public synchronized final void incrementProgressBy(int diff) {
        setProgress(progress + diff);
    }

    public synchronized final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(secondaryProgress + diff);
    }

    public synchronized boolean isIndeterminate() {
        return indeterminate;
    }

    public synchronized void setIndeterminate(boolean indeterminate) {
        if (this.indeterminate != indeterminate) {
            this.indeterminate = indeterminate;
            invalidate();
        }
    }

    public Drawable getProgressDrawable() {
        return progressDrawable;
    }

    public void setProgressDrawable(Drawable d) {
        if (this.progressDrawable != null) {
            this.progressDrawable.setCallback(null);
        }
        this.progressDrawable = d;
        if (d != null) {
            d.setCallback(this);
            if (progressTintList != null) d.setTintList(progressTintList);
        }
        invalidate();
    }

    public void setProgressDrawableTiled(Drawable d) {
        setProgressDrawable(d);
    }

    public Drawable getIndeterminateDrawable() {
        return indeterminateDrawable;
    }

    public void setIndeterminateDrawable(Drawable d) {
        if (this.indeterminateDrawable != null) {
            this.indeterminateDrawable.setCallback(null);
        }
        this.indeterminateDrawable = d;
        if (d != null) {
            d.setCallback(this);
            if (indeterminateTintList != null) d.setTintList(indeterminateTintList);
        }
        invalidate();
    }

    public void setIndeterminateDrawableTiled(Drawable d) {
        setIndeterminateDrawable(d);
    }

    public void setProgressTintList(ColorStateList tint) {
        this.progressTintList = tint;
        if (progressDrawable != null) progressDrawable.setTintList(tint);
        invalidate();
    }

    public ColorStateList getProgressTintList() {
        return progressTintList;
    }

    public void setProgressTintMode(PorterDuff.Mode tintMode) {
        this.progressTintMode = tintMode;
        if (progressDrawable != null) progressDrawable.setTintMode(tintMode);
        invalidate();
    }

    public PorterDuff.Mode getProgressTintMode() {
        return progressTintMode;
    }

    public void setProgressBackgroundTintList(ColorStateList tint) {
        this.progressBackgroundTintList = tint;
        invalidate();
    }

    public ColorStateList getProgressBackgroundTintList() {
        return progressBackgroundTintList;
    }

    public void setIndeterminateTintList(ColorStateList tint) {
        this.indeterminateTintList = tint;
        if (indeterminateDrawable != null) indeterminateDrawable.setTintList(tint);
        invalidate();
    }

    public ColorStateList getIndeterminateTintList() {
        return indeterminateTintList;
    }

    protected void setProgressInternal(int progress, boolean fromUser) {
        int clamped = Math.max(0, Math.min(max, progress));
        if (this.progress != clamped) {
            this.progress = clamped;
            onProgressChanged(clamped, fromUser);
            invalidate();
        }
    }

    protected void onProgressChanged(int progress, boolean fromUser) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth() - paddingLeft - paddingRight;
        int cy = getHeight() / 2;
        if (w <= 0) return;

        float barH = 6f;
        float top = cy - barH / 2f;
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(0xFFE0E2EC);
        canvas.drawRoundRect(paddingLeft, top, paddingLeft + w, top + barH, 3f, 3f, trackPaint);

        float frac = max > 0 ? (float) progress / max : 0f;
        float fillW = w * frac;
        if (fillW > 0) {
            trackPaint.setColor(0xFF4F5BD5);
            canvas.drawRoundRect(paddingLeft, top, paddingLeft + fillW, top + barH, 3f, 3f, trackPaint);
        }
    }
}
