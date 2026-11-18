package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public class ProgressBar extends View {
    protected int max = 100;
    protected int progress = 0;
    protected boolean indeterminate = false;
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

    public synchronized int getMax() {
        return max;
    }

    public synchronized void setMax(int max) {
        this.max = Math.max(1, max);
        if (progress > this.max) progress = this.max;
        invalidate();
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        setProgressInternal(progress, false);
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
