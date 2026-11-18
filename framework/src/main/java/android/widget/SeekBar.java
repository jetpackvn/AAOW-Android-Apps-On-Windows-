package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Thanh kéo SeekBar tương tác trực tiếp bằng chuột trên Windows.
 */
public class SeekBar extends AbsSeekBar {
    public interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(SeekBar seekBar);
        void onStopTrackingTouch(SeekBar seekBar);
    }

    private OnSeekBarChangeListener listener;

    public SeekBar(Context context) {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setMinimumHeight(34);
        if (paddingLeft == 0 && paddingRight == 0) {
            setPadding(14, 6, 14, 6);
        }
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        this.listener = l;
    }

    @Override
    protected void onProgressChanged(int progress, boolean fromUser) {
        if (listener != null) {
            listener.onProgressChanged(this, progress, fromUser);
        }
    }

    private void updateFromTouch(float x) {
        int avail = getWidth() - paddingLeft - paddingRight;
        if (avail <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, (x - paddingLeft) / avail));
        setProgressInternal(Math.round(ratio * max), true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                setPressed(true);
                if (listener != null) listener.onStartTrackingTouch(this);
                updateFromTouch(event.getX());
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.getX());
                return true;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setPressed(false);
                updateFromTouch(event.getX());
                if (listener != null) listener.onStopTrackingTouch(this);
                return true;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth() - paddingLeft - paddingRight;
        if (w <= 0) return;
        float frac = max > 0 ? (float) progress / max : 0f;
        float cx = paddingLeft + w * frac;
        float cy = getHeight() / 2f;

        if (isPressed()) {
            trackPaint.setStyle(Paint.Style.FILL);
            trackPaint.setColor(Color.argb(45, 79, 91, 213));
            canvas.drawCircle(cx, cy, 14f, trackPaint);
        }
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(0xFF4F5BD5);
        canvas.drawCircle(cx, cy, 9f, trackPaint);
    }
}
