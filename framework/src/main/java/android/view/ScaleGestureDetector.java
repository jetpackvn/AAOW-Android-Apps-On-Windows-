package android.view;

import android.content.Context;
import android.os.Handler;

public class ScaleGestureDetector {
    public interface OnScaleGestureListener {
        boolean onScale(ScaleGestureDetector detector);

        boolean onScaleBegin(ScaleGestureDetector detector);

        void onScaleEnd(ScaleGestureDetector detector);
    }

    public static class SimpleOnScaleGestureListener implements OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }

    private final Context context;
    private final OnScaleGestureListener listener;
    private float focusX;
    private float focusY;
    private float currSpan;
    private float prevSpan;
    private float currSpanX;
    private float currSpanY;
    private float prevSpanX;
    private float prevSpanY;
    private long currTime;
    private long prevTime;
    private boolean inProgress;
    private boolean quickScaleEnabled = true;
    private boolean stylusScaleEnabled = true;

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener) {
        this(context, listener, null);
    }

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener, Handler handler) {
        this.context = context;
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event == null || listener == null) return false;
        currTime = event.getEventTime();
        int count = event.getPointerCount();
        int action = event.getActionMasked();

        if (count < 2 || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (inProgress) {
                listener.onScaleEnd(this);
                inProgress = false;
            }
            return true;
        }

        float sumX = 0f, sumY = 0f;
        for (int i = 0; i < count; i++) {
            sumX += event.getX(i);
            sumY += event.getY(i);
        }
        focusX = sumX / count;
        focusY = sumY / count;

        float devSumX = 0f, devSumY = 0f;
        for (int i = 0; i < count; i++) {
            devSumX += Math.abs(event.getX(i) - focusX);
            devSumY += Math.abs(event.getY(i) - focusY);
        }
        float spanX = (devSumX / count) * 2f;
        float spanY = (devSumY / count) * 2f;
        float span = (float) Math.hypot(spanX, spanY);

        if (!inProgress) {
            prevSpanX = currSpanX = spanX;
            prevSpanY = currSpanY = spanY;
            prevSpan = currSpan = span;
            prevTime = currTime;
            inProgress = listener.onScaleBegin(this);
        } else {
            currSpanX = spanX;
            currSpanY = spanY;
            currSpan = span;
            if (listener.onScale(this)) {
                prevSpanX = currSpanX;
                prevSpanY = currSpanY;
                prevSpan = currSpan;
                prevTime = currTime;
            }
        }
        return true;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public float getFocusX() {
        return focusX;
    }

    public float getFocusY() {
        return focusY;
    }

    public float getCurrentSpan() {
        return currSpan;
    }

    public float getCurrentSpanX() {
        return currSpanX;
    }

    public float getCurrentSpanY() {
        return currSpanY;
    }

    public float getPreviousSpan() {
        return prevSpan;
    }

    public float getPreviousSpanX() {
        return prevSpanX;
    }

    public float getPreviousSpanY() {
        return prevSpanY;
    }

    public float getScaleFactor() {
        return prevSpan > 0f ? currSpan / prevSpan : 1.0f;
    }

    public long getTimeDelta() {
        return currTime - prevTime;
    }

    public long getEventTime() {
        return currTime;
    }

    public void setQuickScaleEnabled(boolean scales) {
        this.quickScaleEnabled = scales;
    }

    public boolean isQuickScaleEnabled() {
        return quickScaleEnabled;
    }

    public void setStylusScaleEnabled(boolean scales) {
        this.stylusScaleEnabled = scales;
    }

    public boolean isStylusScaleEnabled() {
        return stylusScaleEnabled;
    }
}
